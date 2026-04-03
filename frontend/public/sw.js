self.addEventListener('install', () => {
  self.skipWaiting()
})

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim())
})

self.addEventListener('push', (event) => {
  const data = event.data ? event.data.json() : {}
  const title = data.title || 'POI Alert'
  const options = {
    body: data.body || '',
    icon: '/icons/poi.svg',
    badge: '/icons/poi-badge.svg'
  }
  event.waitUntil(self.registration.showNotification(title, options))
})

self.addEventListener('notificationclick', (event) => {
  event.notification.close()
  event.waitUntil((async () => {
    const allClients = await clients.matchAll({ includeUncontrolled: true, type: 'window' })
    if (allClients.length > 0) {
      const client = allClients[0]
      return client.focus()
    }
    return clients.openWindow('/')
  })())
})
