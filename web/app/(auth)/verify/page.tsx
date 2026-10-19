import OtpForm from '@/components/OtpForm'
import AuthIllustrationPanel from '@/components/AuthIllustrationPanel'
import { isValidPhone, normalizePhone } from '@/lib/phone'

async function Verify({
  searchParams,
}: {
  searchParams: Promise<{ phone?: string | string[] }>
}) {
  const { phone } = await searchParams
  const raw = Array.isArray(phone) ? phone[0] : phone
  const validPhone = raw && isValidPhone(raw) ? normalizePhone(raw) : undefined

  return (
    <section className='flex min-h-screen w-full'>
      <div className='no-scrollbar flex w-full items-start justify-center max-sm:px-6 lg:h-screen lg:w-1/2 lg:overflow-y-auto'>
        <OtpForm phone={validPhone} />
      </div>
      <AuthIllustrationPanel
        illustration="/illustrations/sign-up.svg"
        headline="One last step"
        tagline="Verifying your phone keeps your accounts and balances secure."
      />
    </section>
  )
}

export default Verify
