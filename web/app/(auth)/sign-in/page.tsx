import AuthForm from '@/components/AuthForm'
import AuthIllustrationPanel from '@/components/AuthIllustrationPanel'

function SignIn() {
  return (
    <section className='flex min-h-screen w-full'>
      <div className='no-scrollbar flex w-full items-start justify-center max-sm:px-6 lg:h-screen lg:w-1/2 lg:overflow-y-auto'>
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
