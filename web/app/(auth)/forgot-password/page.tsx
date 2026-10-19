import ForgotPasswordForm from '@/components/ForgotPasswordForm'
import AuthIllustrationPanel from '@/components/AuthIllustrationPanel'

function ForgotPassword() {
  return (
    <section className='flex min-h-screen w-full'>
      <div className='no-scrollbar flex w-full items-start justify-center max-sm:px-6 lg:h-screen lg:w-1/2 lg:overflow-y-auto'>
        <ForgotPasswordForm />
      </div>
      <AuthIllustrationPanel
        illustration="/illustrations/sign-in.svg"
        headline="Back in no time"
        tagline="A code to your phone is all it takes to set a new password."
      />
    </section>
  )
}

export default ForgotPassword
