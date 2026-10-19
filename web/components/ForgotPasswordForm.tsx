'use client';
import Link from 'next/link'
import Image from 'next/image'
import { useState } from 'react'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useRouter } from 'next/navigation'
import { Button } from '@/components/ui/button'
import { Form, FormControl, FormField, FormLabel, FormMessage } from '@/components/ui/form'
import { Input } from '@/components/ui/input'
import { forgotPasswordFormSchema, resetPasswordFormSchema } from '@/lib/utils'
import { forgotPassword, resetPassword } from '@/lib/api/auth'
import { errorMessage } from '@/lib/api/messages'
import Icon from './Icon'
import Loading03Icon from '@hugeicons/core-free-icons/Loading03Icon'

type RequestValues = z.infer<typeof forgotPasswordFormSchema>
type ResetValues = z.infer<typeof resetPasswordFormSchema>

// Step 1 collects the email and always answers the same way, whether or not
// the account exists (the API never reveals that). Step 2 collects the code
// that was texted to the account's phone, plus the new password.
const ForgotPasswordForm = () => {
    const router = useRouter();
    const [step, setStep] = useState<'request' | 'reset'>('request')
    const [isLoading, setIsLoading] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)

    const requestForm = useForm<RequestValues>({
        resolver: zodResolver(forgotPasswordFormSchema),
        defaultValues: { email: "" },
    })

    const resetForm = useForm<ResetValues>({
        resolver: zodResolver(resetPasswordFormSchema),
        defaultValues: { email: "", code: "", newPassword: "" },
    })

    const onRequest = async (data: RequestValues) => {
        setIsLoading(true)
        setFormError(null)
        try {
            await forgotPassword({ email: data.email })
            resetForm.setValue('email', data.email)
            setStep('reset')
        } catch (error) {
            setFormError(errorMessage(error, 'We could not send a code. Please try again.'))
        } finally {
            setIsLoading(false)
        }
    }

    const onReset = async (data: ResetValues) => {
        setIsLoading(true)
        setFormError(null)
        try {
            await resetPassword(data)
            router.push('/sign-in')
        } catch (error) {
            setFormError(errorMessage(
                error,
                'That code is incorrect or has expired. Request a new code and try again.'
            ))
        } finally {
            setIsLoading(false)
        }
    }

    return (
    <section className="auth-form">
        <header className="flex flex-col gap-5 md:gap-8">
            <Link href="/" className='cursor-pointer flex items-center gap-1'>
                <Image src="/icons/logo.svg" width={34} height={34} alt='Horizon logo' />
                <h1 className='text-26 font-sans font-bold text-black-1'>Horizon</h1>
            </Link>
            <div className="flex flex-col gap-1 md:gap-3">
                <h1 className='text-24 lg:text-36 font-semibold text-gray-900'>
                    Reset Password
                </h1>
                <p className="text-16 font-normal text-gray-600">
                    {step === 'request'
                        ? 'Enter the email on your account and we will text a code to your phone'
                        : `Enter the 6-digit code we texted you and a new password`}
                </p>
            </div>
        </header>

        {step === 'request' ? (
            <Form {...requestForm}>
                <form key="request" onSubmit={requestForm.handleSubmit(onRequest)} className="space-y-4">
                    <FormField
                        control={requestForm.control}
                        name='email'
                        render={({ field }) => (
                            <div className="form-item">
                                <FormLabel className='form-label'>Email</FormLabel>
                                <div className="flex w-full flex-col">
                                    <FormControl>
                                        <Input placeholder='Enter your Email' className='input-class' type='text' {...field} />
                                    </FormControl>
                                    <FormMessage className="form-message mt-2" />
                                </div>
                            </div>
                        )}
                    />

                    <div className='flex flex-col gap-4'>
                        {formError && <p className="form-message" role="alert">{formError}</p>}
                        <Button type="submit" disabled={isLoading} className='form-btn'>
                            {isLoading ? (
                                <>
                                    <Icon icon={Loading03Icon} size={20} className='animate-spin' /> &nbsp; Loading...
                                </>
                            ) : 'Send code'}
                        </Button>
                    </div>
                </form>
            </Form>
        ) : (
            <Form {...resetForm}>
                <form key="reset" onSubmit={resetForm.handleSubmit(onReset)} className="space-y-4">
                    <FormField
                        control={resetForm.control}
                        name='code'
                        render={({ field }) => (
                            <div className="form-item">
                                <FormLabel className='form-label'>Verification Code</FormLabel>
                                <div className="flex w-full flex-col">
                                    <FormControl>
                                        <Input
                                        placeholder='Enter the 6-digit code'
                                        className='input-class'
                                        type='text'
                                        inputMode='numeric'
                                        autoComplete='one-time-code'
                                        maxLength={6}
                                        {...field}
                                        />
                                    </FormControl>
                                    <FormMessage className="form-message mt-2" />
                                </div>
                            </div>
                        )}
                    />

                    <FormField
                        control={resetForm.control}
                        name='newPassword'
                        render={({ field }) => (
                            <div className="form-item">
                                <FormLabel className='form-label'>New Password</FormLabel>
                                <div className="flex w-full flex-col">
                                    <FormControl>
                                        <Input placeholder='Enter your new password' className='input-class' type='password' {...field} />
                                    </FormControl>
                                    <FormMessage className="form-message mt-2" />
                                </div>
                            </div>
                        )}
                    />

                    <div className='flex flex-col gap-4'>
                        {formError && <p className="form-message" role="alert">{formError}</p>}
                        <Button type="submit" disabled={isLoading} className='form-btn'>
                            {isLoading ? (
                                <>
                                    <Icon icon={Loading03Icon} size={20} className='animate-spin' /> &nbsp; Loading...
                                </>
                            ) : 'Reset password'}
                        </Button>
                    </div>
                </form>
            </Form>
        )}

        <footer className="flex justify-center gap-1">
            <p className='text1-4 font-normal text-gray-600'>
                Remembered your password?
            </p>
            <Link href='/sign-in' className='form-link'>
                Sign in
            </Link>
        </footer>
    </section>
)}
export default ForgotPasswordForm
