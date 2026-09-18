# 开发与部署指南

## 环境要求

JDK 17+、Maven 3.9+、Node.js 18+、MySQL 8+。

## 开发

建议使用 `main` 稳定分支，功能使用 `feature/<name>`，修复使用 `fix/<name>`。不要提交密码、JWT 密钥、上传文件和日志。开发环境可用 `DB_PASSWORD` 覆盖数据库密码。

## 发布检查

1. 备份数据库并执行迁移。
2. `mvn clean package` 构建后端 jar。
3. `npm ci && npm run build` 构建前端。
4. 用反向代理将 `/api` 转发至 8080，静态文件指向 `frontend/dist`。
5. 检查 CORS、HTTPS、上传目录权限和数据库权限。
