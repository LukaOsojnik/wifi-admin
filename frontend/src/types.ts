// Mirrors the backend OpenAPI model (WifiConfiguration / enums / ErrorBody).

export type WifiBand = 'BAND_2_4_GHZ' | 'BAND_5_GHZ'

export type EncryptionType =
  | 'OPEN'
  | 'WEP'
  | 'WPA_PSK'
  | 'WPA2_PSK'
  | 'WPA3_SAE'
  | 'WPA2_ENTERPRISE'

export interface WifiConfiguration {
  cpeId: string
  wifiBand: WifiBand
  ssid: string
  encryptionType?: EncryptionType | null
  password?: string | null
}

export interface ErrorBody {
  message?: string
  code?: string
}

export const WIFI_BANDS: WifiBand[] = ['BAND_2_4_GHZ', 'BAND_5_GHZ']

export const ENCRYPTION_TYPES: EncryptionType[] = [
  'OPEN',
  'WEP',
  'WPA_PSK',
  'WPA2_PSK',
  'WPA3_SAE',
  'WPA2_ENTERPRISE',
]
