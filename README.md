# PDF Sign — MVP de assinatura de PDFs

Multi-tenant: cadastro cria empresa → upload de PDF → assinatura eletrônica simples ou certificado A1 → convidados assinam por link mágico (e-mail).

## Stack

- Backend: Java 21 + Spring Boot 3
- Frontend: React + Vite + TypeScript
- DB: PostgreSQL 16
- Deploy: Coolify (Docker Compose)

## Desenvolvimento local

```bash
# Postgres (+ Mailhog opcional)
docker compose up -d

cd backend
# defina APP_JWT_SECRET e APP_CRYPTO_SECRET (>= 32 chars)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

cd frontend
npm install
npm run dev
```

Frontend: http://localhost:5173 — API via proxy `/api`.

## Deploy no Coolify

1. Conecte este repositório (GitHub App / Deploy Key).
2. New Resource → Git → build pack **Docker Compose**.
3. Compose file: `docker-compose.coolify.yml`.
4. Projeto Coolify sugerido: **Assinaturas**.
5. Domínio público no serviço **frontend** + SSL.
6. Variáveis (ver `.env.example`):
   - `POSTGRES_PASSWORD`, `APP_JWT_SECRET`, `APP_CRYPTO_SECRET`
   - `APP_PUBLIC_BASE_URL=https://seu-dominio`
   - SMTP (`SMTP_*`, `MAIL_FROM`) — ou deixe `APP_MAIL_LOG_ONLY=true` no início (links nos logs do backend)
7. Confirme volumes `pg_data` e `pdf_storage`.
8. Deploy.

## Smoke test

1. `/register` — criar conta + empresa  
2. Upload PDF → tipo **Eletrônica simples** → signatário → enviar  
3. Abrir link do e-mail (ou log do backend) em `/sign/{token}` e assinar  
4. Em Configurações, cadastrar `.pfx` e testar fluxo **Certificado digital**
