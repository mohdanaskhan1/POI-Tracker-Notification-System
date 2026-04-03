exports.handler = async function(event) {
  const origin = event.headers.origin || '*'
  if (event.httpMethod === 'OPTIONS') {
    return {
      statusCode: 200,
      headers: {
        'Access-Control-Allow-Origin': origin,
        'Access-Control-Allow-Methods': 'GET,POST,PUT,DELETE,OPTIONS',
        'Access-Control-Allow-Headers': '*',
      },
      body: '',
    }
  }
  const base = process.env.TARGET_BASE
  if (!base) {
    return {
      statusCode: 500,
      headers: { 'Access-Control-Allow-Origin': origin },
      body: 'Missing TARGET_BASE env (your ngrok https URL)',
    }
  }
  const prefix = '/.netlify/functions/api-proxy'
  const path = event.path.startsWith(prefix) ? event.path.substring(prefix.length) : event.path
  const qs = event.rawQuery ? `?${event.rawQuery}` : ''
  const url = `${base}${path}${qs}`
  const headers = { ...event.headers }
  delete headers.host
  headers['ngrok-skip-browser-warning'] = 'true'
  try {
    const res = await fetch(url, {
      method: event.httpMethod,
      headers,
      body: ['GET', 'HEAD'].includes(event.httpMethod) ? undefined : event.body,
    })
    const buf = Buffer.from(await res.arrayBuffer())
    const outHeaders = {
      'Access-Control-Allow-Origin': origin,
    }
    res.headers.forEach((v, k) => {
      if (!['content-length', 'content-encoding'].includes(k.toLowerCase())) {
        outHeaders[k] = v
      }
    })
    return {
      statusCode: res.status,
      headers: outHeaders,
      body: buf.toString('base64'),
      isBase64Encoded: true,
    }
  } catch (e) {
    return {
      statusCode: 502,
      headers: { 'Access-Control-Allow-Origin': origin },
      body: 'Upstream error',
    }
  }
}
