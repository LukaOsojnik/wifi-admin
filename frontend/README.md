# WiFi Admin — frontend

Mali React (Vite + TypeScript) frontend za REST API backenda: prijava, dohvat WiFi parametara
po `cpeId` i spremanje izmjena.

## Pokretanje

Preduvjeti: Node.js 20+ i pokrenut backend na `http://localhost:8081` (vidi `../RUNNING.md`).

```bash
cd frontend
npm install
npm run dev
```

Aplikacija se otvara na **http://localhost:5173** (origin koji backend dopušta putem CORS-a).

## Prijava

Koristi HTTP Basic vjerodajnice backenda — dev zadano **`admin` / `admin`**. Vjerodajnice se
NE spremaju u kod; unose se pri prijavi i drže u memoriji te šalju kao `Authorization: Basic` zaglavlje.

## Konfiguracija

- `VITE_API_BASE` — bazni URL backenda (zadano `http://localhost:8081`). Postavi u `.env`:

```
VITE_API_BASE=http://localhost:8081
```

## Ponašanje

- **Dohvati** → `GET /wifi-parameter/{cpeId}` → popuni formu.
- **Spremi** → `PUT /wifi-parameter` → prikaže potvrđenu konfiguraciju.
- Greške s backenda (401 / 400 / 404 / 502) prikazuju se s porukom i kodom (`ErrorBody`).
