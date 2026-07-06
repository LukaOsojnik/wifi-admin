import { useState, type FormEvent } from 'react'
import { ApiError, getWifiParameter, updateWifiParameter, type Credentials } from './api'
import { ENCRYPTION_TYPES, WIFI_BANDS, type WifiConfiguration } from './types'

export default function App() {
  const [creds, setCreds] = useState<Credentials | null>(null)
  return creds ? (
    <Dashboard creds={creds} onLogout={() => setCreds(null)} />
  ) : (
    <Login onLogin={setCreds} />
  )
}

function Login({ onLogin }: { onLogin: (c: Credentials) => void }) {
  const [username, setUsername] = useState('admin')
  const [password, setPassword] = useState('')

  return (
    <div className="card">
      <h1>WiFi Admin — prijava</h1>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          onLogin({ username, password })
        }}
      >
        <label>
          Korisničko ime
          <input value={username} onChange={(e) => setUsername(e.target.value)} />
        </label>
        <label>
          Lozinka
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </label>
        <button type="submit">Prijava</button>
        <p className="hint">Dev vjerodajnice: admin / admin</p>
      </form>
    </div>
  )
}

function Dashboard({ creds, onLogout }: { creds: Credentials; onLogout: () => void }) {
  const [cpeId, setCpeId] = useState('')
  const [config, setConfig] = useState<WifiConfiguration | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function load(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setInfo(null)
    setConfig(null)
    setLoading(true)
    try {
      setConfig(await getWifiParameter(cpeId.trim(), creds))
    } catch (err) {
      setError(describe(err))
    } finally {
      setLoading(false)
    }
  }

  async function save(e: FormEvent) {
    e.preventDefault()
    if (!config) return
    setError(null)
    setInfo(null)
    setLoading(true)
    try {
      setConfig(await updateWifiParameter(config, creds))
      setInfo('Spremljeno.')
    } catch (err) {
      setError(describe(err))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <header className="row">
        <h1>WiFi Admin</h1>
        <button className="link" onClick={onLogout}>
          Odjava
        </button>
      </header>

      <form onSubmit={load} className="lookup">
        <input
          placeholder="cpeId (npr. CPE_001)"
          value={cpeId}
          onChange={(e) => setCpeId(e.target.value)}
        />
        <button type="submit" disabled={loading || !cpeId.trim()}>
          Dohvati
        </button>
      </form>

      {error && <p className="error">{error}</p>}
      {info && <p className="info">{info}</p>}

      {config && (
        <form onSubmit={save} className="config">
          <label>
            cpeId
            <input value={config.cpeId} disabled />
          </label>
          <label>
            Pojas
            <select
              value={config.wifiBand}
              onChange={(e) => setConfig({ ...config, wifiBand: e.target.value as WifiConfiguration['wifiBand'] })}
            >
              {WIFI_BANDS.map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </select>
          </label>
          <label>
            SSID
            <input
              value={config.ssid}
              onChange={(e) => setConfig({ ...config, ssid: e.target.value })}
            />
          </label>
          <label>
            Šifriranje
            <select
              value={config.encryptionType ?? 'OPEN'}
              onChange={(e) =>
                setConfig({ ...config, encryptionType: e.target.value as WifiConfiguration['encryptionType'] })
              }
            >
              {ENCRYPTION_TYPES.map((t) => (
                <option key={t} value={t}>
                  {t}
                </option>
              ))}
            </select>
          </label>
          <label>
            Lozinka
            <input
              value={config.password ?? ''}
              onChange={(e) => setConfig({ ...config, password: e.target.value })}
            />
          </label>
          <button type="submit" disabled={loading}>
            Spremi
          </button>
        </form>
      )}
    </div>
  )
}

function describe(err: unknown): string {
  if (err instanceof ApiError) {
    return err.code ? `${err.message} (${err.code})` : err.message
  }
  return 'Greška u komunikaciji s poslužiteljem (je li backend pokrenut?)'
}
