import type { WifiConfiguration, ErrorBody } from './types'

// Backend base URL (override with VITE_API_BASE in an .env file).
const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8081'

export interface Credentials {
  username: string
  password: string
}

function authHeader(creds: Credentials): string {
  return 'Basic ' + btoa(`${creds.username}:${creds.password}`)
}

/** Carries the backend's HTTP status + ErrorBody.code so the UI can show meaningful messages. */
export class ApiError extends Error {
  status: number
  code?: string
  constructor(status: number, message: string, code?: string) {
    super(message)
    this.status = status
    this.code = code
  }
}

async function handle(res: Response): Promise<WifiConfiguration> {
  if (res.ok) {
    return (await res.json()) as WifiConfiguration
  }
  let body: ErrorBody = {}
  try {
    body = (await res.json()) as ErrorBody
  } catch {
    // no JSON body (e.g. 401 challenge)
  }
  const message =
    body.message ?? (res.status === 401 ? 'Neispravne vjerodajnice' : `Greška (${res.status})`)
  throw new ApiError(res.status, message, body.code)
}

export async function getWifiParameter(
  cpeId: string,
  creds: Credentials,
): Promise<WifiConfiguration> {
  const res = await fetch(`${API_BASE}/wifi-parameter/${encodeURIComponent(cpeId)}`, {
    headers: { Authorization: authHeader(creds) },
  })
  return handle(res)
}

export async function updateWifiParameter(
  config: WifiConfiguration,
  creds: Credentials,
): Promise<WifiConfiguration> {
  const res = await fetch(`${API_BASE}/wifi-parameter`, {
    method: 'PUT',
    headers: {
      Authorization: authHeader(creds),
      'Content-Type': 'application/json',
    },
    body: JSON.stringify(config),
  })
  return handle(res)
}
