'use client';
import { useEffect } from 'react'
import { refresh } from '@/lib/api/auth'
import { GATE_COOKIE, SESSION_EXPIRED_URL } from '@/lib/session'

// Renders nothing. The refresh cookie is only sent to /api/v1/auth/*, so a
// server component cannot use it; this page does it from the browser, then
// reloads the page the user asked for.
const SessionRefresh = ({ next }: { next: string }) => {
    useEffect(() => {
        let cancelled = false
        refresh().then((ok) => {
            if (cancelled) return
            if (ok) {
                // Marker so the (root) layout goes to sign-in, not back here, if the
                // refreshed session is still rejected.
                document.cookie = `${GATE_COOKIE}=1; path=/; max-age=30; samesite=strict`
                window.location.replace(next)
            } else {
                window.location.replace(SESSION_EXPIRED_URL)
            }
        })
        return () => { cancelled = true }
    }, [next])

    return null
}

export default SessionRefresh
