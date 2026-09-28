import OtpForm from '@/components/OtpForm'
import { isValidPhone, normalizePhone } from '@/lib/phone'
import React from 'react'

async function Verify({
  searchParams,
}: {
  searchParams: Promise<{ phone?: string | string[] }>
}) {
  const { phone } = await searchParams
  const raw = Array.isArray(phone) ? phone[0] : phone
  const validPhone = raw && isValidPhone(raw) ? normalizePhone(raw) : undefined

  return (
    <section className='flex-center size-full max-sm:px-6'>
      <OtpForm phone={validPhone}/>
    </section>
  )
}

export default Verify
