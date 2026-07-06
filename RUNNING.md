# Pokretanje wifi-admin backenda

Upute za lokalno pokretanje REST backenda koji je wrapper oko SOAP platforme.
Arhitektura: REST (JSON) na portu **8081** → backend → SOAP 1.1 (XML) → mock platforme na portu **8080**.

## 1. Preduvjeti

- **JDK 25** (projekt koristi virtualne niti i novije jezične značajke).
- **Docker** + **Docker Compose** (za mock vanjske platforme).
- Maven nije potrebno instalirati — koristi se priloženi wrapper (`./mvnw`).

## 2. Pokretanje mock platforme

Mock predstavlja vanjsku SOAP platformu koju backend poziva. Pokrenite ga **prvi**, kako bi
pozivi prema backendu imali s čime razgovarati:

```bash
docker compose up -d
```

- Mock je dostupan na **http://localhost:8080/platform** (SOAP 1.1 preko HTTP-a).
- Sadrži ~12 seed zapisa: `CPE_001` … `CPE_012`.
- **Napomena:** `updateCpeId` mijenja podatke u memoriji mocka; **restart kontejnera vraća
  početne seed vrijednosti**.

> Backend se može pokrenuti i bez mocka (SOAP klijent se spaja tek pri prvom pozivu), no tada
> REST pozivi vraćaju **502** jer platforma nije dostupna.

Zaustavljanje:

```bash
docker compose down
```

## 3. Pokretanje aplikacije

```bash
./mvnw spring-boot:run
```

- Backend sluša na **http://localhost:8081**.
- Relevantne postavke (`src/main/resources/application.properties`):
  - `server.port=8081`
  - `platform.soap.endpoint=http://localhost:8080/platform` — adresa SOAP platforme
  - `platform.soap.connect-timeout-ms` (zadano 3000), `platform.soap.receive-timeout-ms` (zadano 5000)

## 4. Baza podataka (cache)

Backend koristi bazu kao **cache** podataka s platforme (H2, datoteka `./data/wifi-admin` — preživljava
restart aplikacije). Ponašanje:

- **Read-through (GET):** ako CPE postoji u bazi, vraća se **iz baze**; ako ne postoji, dohvaća se s
  platforme, sprema u bazu i vraća. Sljedeći `GET` za isti CPE ide iz baze (ne zove platformu).
- **Write-through (PUT):** prvo se ažurira platforma, a zatim se potvrđena konfiguracija sprema u bazu,
  pa je promjena odmah vidljiva na idućem `GET`-u.
- **Restart:** restart *aplikacije* zadržava cache (datoteka); restart *mocka* (`docker compose restart`)
  vraća seed vrijednosti platforme — zbog toga zapis u bazi može privremeno odstupati od svježe
  resetirane platforme dok se ne osvježi (npr. novim `PUT`-om).

> Testovi koriste zasebnu **in-memory** H2 bazu (bez datoteke), pa su izolirani i ponovljivi.

## 5. Sinkronizacija (scheduler)

Noćni posao osvježava **već predmemorirane** CPE-ove iz platforme (najustarije prve — po `lastUpdated`),
kako bi cache ostao svjež bez čekanja na cache miss.

- Konfiguracija (`application.properties`):
  - `sync.enabled` — uključeno/isključeno (zadano `true`; testovi ga gase).
  - `sync.cron` — vrijeme (zadano `0 0 2 * * *`, tj. svaki dan u 02:00).
  - `sync.batch-size` — najveći broj CPE-ova po pokretanju (zadano `50`).
- **Napomena (Option X):** sinkronizira samo CPE-ove koji su već u bazi (populirani prethodnim `GET`-om);
  ne otkriva CPE-ove kojima se nikad nije pristupilo. Prazan cache → posao ništa ne radi.

Demo:

```bash
# 1) privremeno ubrzaj raspored (svaku minutu) i pokreni app
#    npr. --sync.cron="0 * * * * *"
# 2) napuni cache s nekoliko CPE-ova
curl -s -u admin:admin http://localhost:8081/wifi-parameter/CPE_001 > /dev/null
curl -s -u admin:admin http://localhost:8081/wifi-parameter/CPE_002 > /dev/null
# 3) u logu aplikacije pratite: "Nightly sync complete: synced=..., failed=..."
```

## 6. Autentikacija

REST API je zaštićen **HTTP Basic** autentikacijom (bez stanja, CSRF isključen jer je riječ o
servisnom JSON API-ju).

- Dev vjerodajnice: **`admin` / `admin`**.
- U produkciji postavite putem env varijabli `SECURITY_USER_NAME` / `SECURITY_USER_PASSWORD`.
- Zahtjev bez ispravnih vjerodajnica vraća **401 Unauthorized**.

Svi `curl` primjeri u nastavku koriste `-u admin:admin`.

## 7. Primjeri poziva (REST API)

### Dohvat parametara — `GET /wifi-parameter/{cpeId}`

```bash
curl -s -u admin:admin http://localhost:8081/wifi-parameter/CPE_001
```

Primjer odgovora (`200 OK`):

```json
{
  "cpeId": "CPE_001",
  "wifiBand": "BAND_2_4_GHZ",
  "ssid": "Office-2G",
  "encryptionType": "WPA2_PSK",
  "password": "seed-wifi-01"
}
```

### Promjena parametara — `PUT /wifi-parameter`

```bash
curl -s -u admin:admin -X PUT http://localhost:8081/wifi-parameter \
  -H "Content-Type: application/json" \
  -d '{
        "cpeId": "CPE_001",
        "wifiBand": "BAND_2_4_GHZ",
        "ssid": "Office-2G",
        "encryptionType": "WPA2_PSK",
        "password": "nova-lozinka"
      }'
```

Odgovor je potvrđena konfiguracija (`200 OK`, isti model). Novu lozinku vidljivu je na sljedećem
`GET`-u dok mock radi.

### Primjeri grešaka

**401 — bez vjerodajnica:**

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8081/wifi-parameter/CPE_001
# 401
```

**404 — nepoznat CPE:**

```bash
curl -s -u admin:admin -o /dev/null -w "%{http_code}\n" http://localhost:8081/wifi-parameter/NE_POSTOJI
# 404, tijelo: {"message":"CPE not found: NE_POSTOJI","code":"CPE_NOT_FOUND"}
```

**400 — nedostaje lozinka za tip šifriranja koji je zahtijeva:**

```bash
curl -s -u admin:admin -X PUT http://localhost:8081/wifi-parameter \
  -H "Content-Type: application/json" \
  -d '{"cpeId":"CPE_001","wifiBand":"BAND_2_4_GHZ","ssid":"Net","encryptionType":"WPA2_PSK"}'
# 400, tijelo: {"message":"Password is required for encryption type WPA2_PSK","code":"PASSWORD_REQUIRED"}
```

**502 — platforma nedostupna:** ako mock nije pokrenut, REST pozivi vraćaju `502` s
`{"code":"PLATFORM_UNAVAILABLE"}`.

## 8. Testovi

```bash
# Jedinični testovi (brzo, ne zahtijevaju Docker) — Surefire, klase *Test
./mvnw test

# Jedinični + integracijski testovi — Failsafe, klase *IT
# ZAHTIJEVA pokrenut mock (docker compose up -d)
./mvnw verify
```

- `*Test` (Surefire) — mapper, validator, servisna logika; ne diraju mrežu.
- `*IT` (Failsafe) — pravi pozivi prema mock platformi (`SoapClientIT`, `WifiControllerIT`).
