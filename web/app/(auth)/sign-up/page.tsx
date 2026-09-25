import AuthForm from '@/components/AuthForm'
import AuthIllustrationPanel from '@/components/AuthIllustrationPanel'

function SignUp() {
  return (
    <section className='flex min-h-screen w-full'>
      <div className='flex-center flex-1 max-sm:px-6'>
        <AuthForm type="sign-up"/>
      </div>
      <AuthIllustrationPanel
        illustration="/illustrations/sign-up.svg"
        headline="Built for Uganda and DR Congo"
        tagline="One account for your bank cards and mobile money, in UGX, CDF or USD."
      />
    </section>
  )
}

export default SignUp
