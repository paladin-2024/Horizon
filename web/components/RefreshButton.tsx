'use client'
import { useEffect, useState, useTransition } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from './ui/button'
import Icon from './Icon'
import RefreshIcon from '@hugeicons/core-free-icons/RefreshIcon'
import Clock01Icon from '@hugeicons/core-free-icons/Clock01Icon'

function formatAgo(seconds: number): string {
    if (seconds < 2) return 'Updated just now'
    if (seconds < 60) return `Updated ${seconds}s ago`
    const minutes = Math.floor(seconds / 60)
    return `Updated ${minutes}m ago`
}

/**
 * Re-fetches the server-rendered page's data in place — no fake "live"
 * polling. The "Updated Xs ago" label is real elapsed time since this
 * component mounted (i.e. since the page last rendered server-side),
 * ticking every second like a real status readout rather than a static label.
 */
const RefreshButton = () => {
    const router = useRouter()
    const [isPending, startTransition] = useTransition()
    const [seconds, setSeconds] = useState(0)

    useEffect(() => {
        const interval = setInterval(() => setSeconds((s) => s + 1), 1000)
        return () => clearInterval(interval)
    }, [])

    const onRefresh = () => {
        startTransition(() => {
            router.refresh()
            setSeconds(0)
        })
    }

    return (
        <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 rounded-full border border-gray-200 px-3 py-1.5 text-12 font-medium text-gray-600">
                <Icon icon={Clock01Icon} size={14} />
                {formatAgo(seconds)}
            </span>
            <Button
                type="button"
                variant="outline"
                onClick={onRefresh}
                disabled={isPending}
                className="h-auto gap-1.5 rounded-full border-gray-200 px-3 py-1.5 text-12 font-medium text-gray-600"
            >
                <Icon icon={RefreshIcon} size={14} className={isPending ? 'animate-spin' : ''} />
                Refresh
            </Button>
        </div>
    )
}

export default RefreshButton
