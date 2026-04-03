import { Capacitor } from '@capacitor/core'
import apiClient, { getApiBaseUrl } from '../utils/api'

const API_BASE = getApiBaseUrl()
let isReporting = false

function isNative() {
  return Capacitor.isNativePlatform ? Capacitor.isNativePlatform() : Capacitor.getPlatform() !== 'web'
}

async function ensurePermissions() {
  const { LocalNotifications } = await import('@capacitor/local-notifications')
  await LocalNotifications.requestPermissions()
}

export async function startBackgroundTracking(options = {}) {
  if (!isNative()) return null
  const mod = await new Function('return import("@capacitor-community/background-geolocation")')()
  const { BackgroundGeolocation } = mod
  await ensurePermissions()
  const distanceFilter = options.distanceFilter || 50
  const radius = options.radius || 300
  const categories = options.categories ? options.categories.join(',') : 'fuel,restaurant,shopping_mall'
  const deviceId = options.deviceId || 'device-' + Math.random().toString(36).slice(2, 10)
  const watcherId = await BackgroundGeolocation.addWatcher({
    backgroundMessage: 'Tracking location for POIs',
    backgroundTitle: 'POI Tracking',
    requestPermissions: true,
    stale: false,
    distanceFilter: 20
  }, async (location, error) => {
    if (error) return
    if (isReporting) return
    isReporting = true
    const payload = {
      deviceId,
      latitude: location.latitude,
      longitude: location.longitude,
      timestamp: Date.now(),
      radius,
      categories
    }
    try {
      const res = await apiClient.post(`${API_BASE}/api/locations/report`, payload)
      if (res.data && res.data.entered && res.data.poi) {
        const { LocalNotifications } = await import('@capacitor/local-notifications')
        await LocalNotifications.schedule({
          notifications: [{
            id: Math.floor(Math.random() * 1000000),
            title: `Welcome to ${res.data.poi.name}`,
            body: `${res.data.poi.category} • ${Math.round(res.data.poi.distanceMeters)} m`
          }]
        })
      }
    } catch (_) {
    } finally {
      isReporting = false
    }
  })
  return { watcherId, deviceId }
}

export async function stopBackgroundTracking(watcher) {
  if (!isNative()) return
  if (!watcher || !watcher.watcherId) return
  const mod = await new Function('return import("@capacitor-community/background-geolocation")')()
  const { BackgroundGeolocation } = mod
  await BackgroundGeolocation.removeWatcher({ id: watcher.watcherId })
}

export function isCapacitorNative() {
  return isNative()
}
