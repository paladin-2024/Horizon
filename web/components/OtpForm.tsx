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
import { otpFormSchema } from '@/lib/utils'
import { resendOtp, verifyOtp } from '@/lib/api/auth'
import { errorMessage } from '@/lib/api/messages'
import { normalizePhone } from '@/lib/phone'
import Icon from './Icon'
import Loading03Icon from '@hugeicons/core-free-icons/Loading03Icon'

type OtpFormValues = z.infer<typeof otpFormSchema>

// `phone` comes from the sign-up redirect (/verify?phone=...). When it is
// missing (for example an unverified user who signed in) the form asks for it.
const OtpForm = ({ phone }: { phone?: string }) => {
    const router = useRouter();
    const [isLoading, setIsLoading] = useState(false)
    const [isResending, setIsResending] = useState(false)
    const [formError, setFormError] = useState<string | null>(null)
    const [notice, setNotice] = useState<string | null>(null)

    const form = useForm<OtpFormValues>({
        resolver: zodResolver(otpFormSchema),
        defaultValues: {
            phone: phone ?? "",
            code: "",
        },
    })

    const onSubmit = async (data: OtpFormValues) => {
        setIsLoading(true)
        setFormError(null)
        setNotice(null)
        try {
            await verifyOtp({ phone: normalizePhone(data.phone), code: data.code })
            router.push('/')
            router.refresh()
        } catch (error) {
            setFormError(errorMessage(
                error,
                'That code is incorrect or has expired. Request a new code and try again.'
            ))
        } finally {
            setIsLoading(false)
        }
    }

    const onResend = async () => {
        setFormError(null)
        setNotice(null)
        if (!(await form.trigger('phone'))) return
        setIsResending(true)
        try {
            await resendOtp({ phone: normalizePhone(form.getValues('phone')) })
            setNotice('A new code is on its way.')
        } catch (error) {
            setFormError(errorMessage(error, 'We could not send a new code. Please try again.'))
        } finally {
            setIsResending(false)
        }
    }

    return (
    <section className="auth-form">
        <header className="flex flex-col gap-5 md:gap-8">
            <Link  href="/" className='cursor-pointer flex items-center gap-1'>
                <Image
                    src="/icons/logo.svg"
                    width={34}
                    height={34}
                    alt='Horizon logo'
                />
                <h1 className='text-26 font-sans font-bold text-black-1'>Horizon</h1>
            </Link>
            <div className="flex flex-col gap-1 md:gap-3">
                <h1 className='text-24 lg:text-36 font-semibold text-gray-900'>
                    Verify Phone
                </h1>

                <p className="text-16 font-normal text-gray-600">
                    {phone
                        ? `We sent a 6-digit code to ${phone}`
                        : 'Enter your phone number and the 6-digit code we sent you'
                    }
                </p>
            </div>
        </header>

        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                {!phone && (
                    <FormField
                        control={form.control}
                        name='phone'
                        render={({ field }) => (
                            <div className="form-item">
                                <FormLabel className='form-label'>
                                    Phone Number
                                </FormLabel>
                                <div className="flex w-full flex-col">
                                    <FormControl>
                                        <Input
                                        placeholder='Example: +256771234567'
                                        className='input-class'
                                        type='text'
                                        {...field}
                                        />
                                    </FormControl>
                                    <FormMessage className="form-message mt-2" />
                                </div>
                            </div>
                        )}
                    />
                )}

                <FormField
                    control={form.control}
                    name='code'
                    render={({ field }) => (
                        <div className="form-item">
                            <FormLabel className='form-label'>
                                Verification Code
                            </FormLabel>
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

                <div className='flex flex-col gap-4'>

                {formError && (
                    <p className="form-message" role="alert">{formError}</p>
                )}
                {notice && (
                    <p className="text-14 font-normal text-gray-600" role="status">{notice}</p>
                )}

                <Button type="submit" disabled={isLoading} className='form-btn'>
                    {isLoading ?(
                        <>
                            <Icon
                                icon={Loading03Icon}
                                size={20}
                                className='animate-spin'
                            /> &nbsp; Loading...
                        </>
                    ): 'Verify'}
                </Button>

                </div>
            </form>
        </Form>

        <footer className="flex justify-center gap-1">
            <p className='text1-4 font-normal text-gray-600'>
                Didn&apos;t get a code?
            </p>
            <button type="button" onClick={onResend} disabled={isResending} className='form-link'>
                Resend code
            </button>
        </footer>
    </section>
)}
export default OtpForm
