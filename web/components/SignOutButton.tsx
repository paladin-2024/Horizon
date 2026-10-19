'use client'
import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button } from './ui/button'
import Icon from './Icon'
import Logout03Icon from '@hugeicons/core-free-icons/Logout03Icon'
import Loading03Icon from '@hugeicons/core-free-icons/Loading03Icon'
import { logout } from '@/lib/api/auth'

const SignOutButton = () => {
  const router = useRouter()
  const [isLoading, setIsLoading] = useState(false)

  const onSignOut = async () => {
    setIsLoading(true)
    try {
      await logout()
    } catch {
      // The cookies may already be gone (expired session); sign out locally regardless.
    } finally {
      router.push('/sign-in')
      router.refresh()
    }
  }

  return (
    <Button variant="outline" className="gap-2" disabled={isLoading} onClick={onSignOut}>
      <Icon icon={isLoading ? Loading03Icon : Logout03Icon} size={18} className={isLoading ? 'animate-spin' : ''} />
      Sign out
    </Button>
  )
}

export default SignOutButton
