'use client'
import { useState, useTransition } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from './ui/button'
import Icon from './Icon'
import RefreshIcon from '@hugeicons/core-free-icons/RefreshIcon'
import Clock01Icon from '@hugeicons/core-free-icons/Clock01Icon'

/** Re-fetches the server-rendered page's data in place — no fake "live" polling. */
const RefreshButton = () => {
    const router = useRouter()
    const [isPending, startTransition] = useTransition()
    const [justRefreshed, setJustRefreshed] = useState(false)

    const onRefresh = () => {
        startTransition(() => {
            router.refresh()
            setJustRefreshed(true)
            setTimeout(() => setJustRefreshed(false), 2000)
        })
    }

    return (
        <div className="flex items-center gap-2">
            <span className="flex items-center gap-1.5 rounded-full border border-gray-200 px-3 py-1.5 text-12 font-medium text-gray-600">
                <Icon icon={Clock01Icon} size={14} />
                {justRefreshed ? 'Updated just now' : 'Up to date'}
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
