# SM Apps PostgreSQL 搭建指导书

## 1. 建设目标

本项目采用两套相互独立的 PostgreSQL 实例：**远程实例**部署在阿里云 Ubuntu 22.04 服务器上，承担正式环境数据存储；**本机实例**部署在 WSL Ubuntu 中，承担日常开发、联调与集成测试。两套实例使用一致的 PostgreSQL 主版本和数据库命名，但不直接同步数据，正式数据不得复制到开发环境。

本指导书统一使用 **PostgreSQL 18**、默认端口 **5432**、数据库名 **sm_chat**、业务角色 **sm_chat_app**。命令中的密码由操作者现场输入，不把真实密码写入本文、Shell 历史、Git 仓库或 Maven 配置。

## 2. 部署边界

| 环境 | 操作系统 | 用途 | 监听地址 | 公网端口 |
|---|---|---|---|---|
| 阿里云服务器 | Ubuntu 22.04 | 正式环境 | `localhost` | 不开放 `5432` |
| 本机 WSL | 以实际版本为准 | 开发和集成测试 | `localhost` | 不涉及安全组 |

远程聊天服务与远程 PostgreSQL 位于同一台服务器时，应用通过 `127.0.0.1:5432` 访问数据库。需要从本机临时管理远程数据库时，使用 SSH 本地端口转发，不修改 PostgreSQL 监听范围，也不新增阿里云安全组规则。

## 3. 安装前检查

在远程服务器和 WSL Ubuntu 中分别执行：

```bash
cat /etc/os-release
uname -m
free -h
df -h /
timedatectl
```

期望结果：

- **系统版本:** 远程服务器为 Ubuntu 22.04；WSL 应为 PostgreSQL 官方仓库当前支持的 Ubuntu 版本。
- **处理器架构:** 通常为 `x86_64`，对应软件包架构 `amd64`。
- **磁盘空间:** 开发环境至少预留 5 GB；远程环境应根据文件消息元数据、聊天记录与备份增长量预留空间。
- **系统时区:** 操作系统可保持 `Asia/Shanghai`，数据库会统一使用 `UTC` 保存时间。

如果系统中已经存在 PostgreSQL，先确认版本和集群，不要直接重复安装：

```bash
psql --version 2>/dev/null || true
pg_lsclusters 2>/dev/null || true
sudo ss -lntp | grep ':5432' || true
```

## 4. 安装 PostgreSQL 18

以下步骤在远程 Ubuntu 和 WSL Ubuntu 中各执行一次。使用 PostgreSQL 官方 PGDG APT 仓库，从而明确锁定主版本，而不是使用 Ubuntu 随发行版冻结的版本。

```bash
sudo apt update
sudo apt install -y postgresql-common ca-certificates
sudo /usr/share/postgresql-common/pgdg/apt.postgresql.org.sh
sudo apt update
sudo apt install -y postgresql-18 postgresql-client-18
```

确认安装结果：

```bash
psql --version
pg_lsclusters
sudo systemctl status postgresql --no-pager
```

应看到 **版本:18.x**、**集群:18/main**、**状态:online**。Ubuntu 的 `postgresql` 是集群管理服务，具体实例通常由 `postgresql@18-main` 管理。

## 5. WSL 的 systemd 设置

先在 WSL 中检查：

```bash
systemctl is-system-running
```

如果提示系统不是由 systemd 启动，在 WSL 中编辑配置：

```bash
sudo nano /etc/wsl.conf
```

写入：

```ini
[boot]
systemd=true
```

保存后退出 WSL，并在 Windows PowerShell 中执行：

```powershell
wsl --shutdown
```

重新进入 Ubuntu，然后执行：

```bash
sudo systemctl enable --now postgresql
systemctl is-active postgresql
```

期望输出为 `active`。

## 6. 创建项目数据库与最小权限角色

### 6.1 远程正式环境

创建一个不能管理其他角色、不能创建其他数据库、不是超级用户的业务角色：

```bash
sudo -u postgres createuser \
  --login \
  --no-superuser \
  --no-createdb \
  --no-createrole \
  --pwprompt \
  sm_chat_app
```

命令会要求输入两次业务数据库密码。随后创建数据库并限定公共权限：

```bash
sudo -u postgres createdb \
  --owner=sm_chat_app \
  --encoding=UTF8 \
  --template=template0 \
  sm_chat

sudo -u postgres psql -d sm_chat <<'SQL'
REVOKE ALL ON DATABASE sm_chat FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE sm_chat TO sm_chat_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO sm_chat_app;
SQL
```

业务程序只能使用 `sm_chat_app`，不得使用 `postgres` 超级用户连接。

### 6.2 WSL 开发环境

重复上述角色和 `sm_chat` 数据库创建步骤，然后额外建立独立测试库：

```bash
sudo -u postgres createdb \
  --owner=sm_chat_app \
  --encoding=UTF8 \
  --template=template0 \
  sm_chat_test

sudo -u postgres psql -d sm_chat_test <<'SQL'
REVOKE ALL ON DATABASE sm_chat_test FROM PUBLIC;
GRANT CONNECT, TEMPORARY ON DATABASE sm_chat_test TO sm_chat_app;
REVOKE CREATE ON SCHEMA public FROM PUBLIC;
GRANT USAGE, CREATE ON SCHEMA public TO sm_chat_app;
SQL
```

开发数据库和测试数据库必须分离，自动化测试只能指向 `sm_chat_test`。

## 7. 安全配置

### 7.1 获取真实配置文件路径

不要根据版本手写路径，直接询问正在运行的实例：

```bash
sudo -u postgres psql -Atc 'SHOW config_file;'
sudo -u postgres psql -Atc 'SHOW hba_file;'
```

### 7.2 限制监听地址并启用 SCRAM

在远程服务器和 WSL 中分别执行：

```bash
sudo -u postgres psql <<'SQL'
ALTER SYSTEM SET listen_addresses = 'localhost';
ALTER SYSTEM SET password_encryption = 'scram-sha-256';
ALTER SYSTEM SET timezone = 'UTC';
SQL
```

使用上一节得到的 `hba_file` 路径打开认证配置：

```bash
sudoedit /etc/postgresql/18/main/pg_hba.conf
```

确认针对项目的 TCP 认证最终包含下列规则，并位于可能匹配同一连接的宽泛规则之前：

```text
# TYPE  DATABASE      USER          ADDRESS          METHOD
host    sm_chat       sm_chat_app   127.0.0.1/32     scram-sha-256
host    sm_chat       sm_chat_app   ::1/128          scram-sha-256
host    sm_chat_test  sm_chat_app   127.0.0.1/32     scram-sha-256
host    sm_chat_test  sm_chat_app   ::1/128          scram-sha-256
```

远程服务器没有 `sm_chat_test` 时，可省略对应两行。不要加入 `0.0.0.0/0`、`::/0` 或 `trust` 规则。

检查配置并重启：

```bash
sudo -u postgres psql -c "SELECT line_number, type, database, user_name, address, auth_method, error FROM pg_hba_file_rules WHERE error IS NOT NULL;"
sudo systemctl restart postgresql
sudo systemctl is-active postgresql
```

第一条查询应返回 **0 行错误**，服务状态应为 `active`。

### 7.3 云端网络边界

阿里云安全组不要放行入方向 TCP `5432`，Ubuntu 防火墙也不需要开放该端口。验证数据库只监听环回地址：

```bash
sudo ss -lntp | grep ':5432'
```

期望只出现 `127.0.0.1:5432` 和可能存在的 `[::1]:5432`，不应出现 `0.0.0.0:5432` 或公网 IP。

## 8. 连接与验收

### 8.1 服务健康检查

```bash
pg_isready -h 127.0.0.1 -p 5432
sudo -u postgres psql -Atc 'SELECT version();'
sudo -u postgres psql -Atc 'SHOW listen_addresses;'
sudo -u postgres psql -Atc 'SHOW password_encryption;'
sudo -u postgres psql -Atc 'SHOW timezone;'
```

期望关键结果：

```text
服务状态: accepting connections
监听地址: localhost
密码算法: scram-sha-256
数据库时区: UTC
```

### 8.2 业务角色登录测试

```bash
psql -h 127.0.0.1 -p 5432 -U sm_chat_app -d sm_chat -W
```

输入密码后，在 `psql` 中执行：

```sql
SELECT current_database(), current_user, current_setting('TimeZone');
CREATE TABLE setup_probe(id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY);
DROP TABLE setup_probe;
\q
```

登录、建表和删除探针表都成功，说明数据库所有权、Schema 权限和 TCP 密码认证配置有效。

### 8.3 WSL 测试库检查

```bash
psql -h 127.0.0.1 -p 5432 -U sm_chat_app -d sm_chat_test -W \
  -c "SELECT current_database(), current_user;"
```

## 9. 从本机安全访问远程数据库

在 Windows PowerShell 中建立 SSH 隧道，保持该终端窗口运行：

```powershell
ssh -N -L 15432:127.0.0.1:5432 aliyun-codex
```

随后新开一个终端，通过本机 `15432` 访问远程 PostgreSQL：

```bash
psql -h 127.0.0.1 -p 15432 -U sm_chat_app -d sm_chat -W
```

该方式只让 PostgreSQL 接受服务器本机连接，数据库流量由 SSH 加密通道承载。若本机 `15432` 已占用，可改成其他未占用的本机端口，右侧远程端口仍保持 `5432`。

## 10. Java 项目连接参数

远程服务端与 WSL 开发服务端都使用以下连接形式：

```text
数据库URL:jdbc:postgresql://127.0.0.1:5432/sm_chat
数据库用户:sm_chat_app
数据库密码:由环境变量注入
```

测试环境使用：

```text
数据库URL:jdbc:postgresql://127.0.0.1:5432/sm_chat_test
数据库用户:sm_chat_app
数据库密码:由环境变量注入
```

临时终端会话可这样设置：

```bash
export SM_CHAT_DB_URL='jdbc:postgresql://127.0.0.1:5432/sm_chat'
export SM_CHAT_DB_USER='sm_chat_app'
read -rsp 'Database password: ' SM_CHAT_DB_PASSWORD
echo
export SM_CHAT_DB_PASSWORD
```

应用启动时读取 `SM_CHAT_DB_URL`、`SM_CHAT_DB_USER` 和 `SM_CHAT_DB_PASSWORD`。密码不得写入 `application.properties`、源码、README、构建日志或 Git 跟踪文件；正式部署时应通过权限为 `0600` 的环境文件或系统密钥设施注入。

## 11. 备份与恢复演练

### 11.1 逻辑备份

在远程服务器建立仅当前管理用户可访问的备份目录：

```bash
install -d -m 700 "$HOME/backups/sm-chat"
pg_dump -h 127.0.0.1 -U sm_chat_app -W \
  --format=custom \
  --file="$HOME/backups/sm-chat/sm_chat_$(date +%F_%H%M%S).dump" \
  sm_chat
```

`custom` 格式便于使用 `pg_restore` 检查内容和选择性恢复。备份文件应定期复制到服务器以外、经过访问控制的存储位置；只存放在同一块云盘上不构成完整备份。

### 11.2 恢复演练

先创建独立恢复验证库，不覆盖正在使用的数据库：

```bash
sudo -u postgres createdb --owner=sm_chat_app sm_chat_restore_test
pg_restore -h 127.0.0.1 -U sm_chat_app -W \
  --exit-on-error \
  --dbname=sm_chat_restore_test \
  "$HOME/backups/sm-chat/备份文件名.dump"
psql -h 127.0.0.1 -U sm_chat_app -W -d sm_chat_restore_test -c '\dt'
```

必须周期性完成恢复演练；只有能够成功恢复并校验数据的备份才可视为有效备份。

## 12. 日常维护

查看服务、集群和最近日志：

```bash
systemctl status postgresql --no-pager
pg_lsclusters
sudo journalctl -u postgresql@18-main -n 100 --no-pager
```

安装 PostgreSQL 18 的小版本安全更新：

```bash
sudo apt update
apt list --upgradable 2>/dev/null | grep postgresql || true
sudo apt upgrade
```

小版本更新应先在 WSL 或独立测试环境验证，再安排远程维护窗口。升级到新的主版本时，需要单独制定迁移、备份、恢复验证和回滚方案，不能把安装新主版本等同于完成数据库升级。

## 13. 最终验收清单

- **数据库版本:** 远程和 WSL 均为 PostgreSQL 18.x。
- **服务状态:** `postgresql` 为 `active`，`18/main` 为 `online`。
- **监听范围:** 仅 `localhost:5432`，阿里云安全组未开放 `5432`。
- **认证算法:** 项目 TCP 连接使用 `scram-sha-256`。
- **权限边界:** 业务使用 `sm_chat_app`，该角色不是超级用户且不能创建角色或数据库。
- **环境隔离:** 正式库为 `sm_chat`，WSL 测试库为 `sm_chat_test`。
- **时间标准:** PostgreSQL 会话默认时区为 `UTC`，业务时间字段使用 `timestamptz`。
- **项目密钥:** 数据库密码未进入 Git、源码、文档或命令历史。
- **恢复能力:** 已完成一次自定义格式备份及独立数据库恢复演练。

## 14. 官方资料

- [PostgreSQL 官方 Ubuntu 安装说明](https://www.postgresql.org/download/linux/ubuntu/)
- [PostgreSQL `pg_hba.conf` 认证规则](https://www.postgresql.org/docs/current/auth-pg-hba-conf.html)
- [PostgreSQL SCRAM-SHA-256 说明](https://www.postgresql.org/about/featurematrix/detail/scram-sha-256-authentication/)
- [PostgreSQL `pg_dump` 文档](https://www.postgresql.org/docs/current/app-pgdump.html)
- [PostgreSQL `pg_dumpall` 文档](https://www.postgresql.org/docs/current/app-pg-dumpall.html)
