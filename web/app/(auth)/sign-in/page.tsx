import AuthForm from '@/components/AuthForm'
import AuthIllustrationPanel from '@/components/AuthIllustrationPanel'

function SignIn() {
  return (
    <section className='flex min-h-screen w-full'>
      <div className='flex-center flex-1 max-sm:px-6'>
        <AuthForm type="sign-in"/>
      </div>
      <AuthIllustrationPanel
        illustration="/illustrations/sign-in.svg"
        headline="Manage every account in one place"
        tagline="Sign in to see your linked banks and mobile money, together."
      />
    </section>
  )
}

export default SignIn
