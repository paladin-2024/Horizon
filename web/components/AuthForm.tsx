'use client';
import Link from 'next/link'
import Image from 'next/image'
import { useState } from 'react'
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import {Form,} from "@/components/ui/form"
import { Separator } from "@/components/ui/separator"

import CustomInput from './CustomInput';
import { authFormSchema } from '@/lib/utils';
import Icon from './Icon';
import Loading03Icon from '@hugeicons/core-free-icons/Loading03Icon';
import { useRouter } from 'next/navigation';
import { signIn, signUp } from '@/lib/actions/user.action';



const AuthForm = ({ type }:{ type: string}) => {
    const router= useRouter();
    const [isLoading, setIsLoading] = useState(false)

    const formSchema = authFormSchema(type);

    const form = useForm<z.infer<typeof formSchema>>({
        resolver: zodResolver(formSchema),
        defaultValues: {
            email: "",
            password: "",
        },
    })

    const onSubmit= async (data: z.infer<typeof formSchema>) => {
        setIsLoading(true)
        try{
            if(type === 'sign-up'){
                await signUp(data);
            }

            if(type === 'sign-in'){
                const response= await signIn({
                    email:data.email,
                    password:data.password,
                })
                if(response) router.push('/')
            }

        }catch(error){
            console.log(error);
        } finally{
            setIsLoading(false);
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
                    {type === 'sign-in' ? 'Sign In' : 'Sign Up'}
                </h1>

                <p className="text-16 font-normal text-gray-600">
                    Please enter your details
                </p>
            </div>
        </header>

        <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
                {type === 'sign-up' && (
                    <>
                        <div className="flex flex-col gap-2">
                            <p className="text-14 flex items-center gap-2 font-semibold text-gray-500">
                                <span className="size-1.5 rounded-full bg-bankGradient" />
                                Personal details
                            </p>
                            <div className="flex gap-4">
                                <CustomInput
                                    control={form.control}
                                    name='firstName'
                                    label='First Name'
                                    placeholder='Enter your First Name'
                                />

                                <CustomInput
                                    control={form.control}
                                    name='lastName'
                                    label='Last Name'
                                    placeholder='Enter your Last Name'
                                />
                            </div>

                            <div className="flex gap-4">
                                <CustomInput
                                    control={form.control}
                                    name='dateOfBirth'
                                    label='Date of Birth'
                                    placeholder='YYYY-MM-DD'
                                />

                                <CustomInput
                                    control={form.control}
                                    name='ssn'
                                    label='National ID'
                                    placeholder='Enter your national ID number'
                                />
                            </div>
                        </div>

                        <Separator />

                        <div className="flex flex-col gap-2">
                            <p className="text-14 flex items-center gap-2 font-semibold text-gray-500">
                                <span className="size-1.5 rounded-full bg-bankGradient" />
                                Address
                            </p>
                            <CustomInput
                                control={form.control}
                                name='address1'
                                label='Address'
                                placeholder='Enter your Specific Address'
                            />

                            <CustomInput
                                control={form.control}
                                name='city'
                                label='City'
                                placeholder='Enter your City'
                            />

                            <div className="flex gap-4">
                                <CustomInput
                                    control={form.control}
                                    name='state'
                                    label='State'
                                    placeholder='Example: Kampala'
                                />

                                <CustomInput
                                    control={form.control}
                                    name='postalCode'
                                    label='Postal Code'
                                    placeholder='Optional'
                                />
                            </div>
                        </div>

                        <Separator />

                        <p className="text-14 flex items-center gap-2 font-semibold text-gray-500">
                            <span className="size-1.5 rounded-full bg-bankGradient" />
                            Account credentials
                        </p>
                    </>
                )}

                <CustomInput
                control={form.control}
                name='email'
                label='Email'
                placeholder='Enter your Email'
                />

                <CustomInput
                control={form.control}
                name='password'
                label='Password'
                placeholder='Enter your password'
                />
                <div className='flex flex-col gap-4'>

                <Button type="submit" disabled={isLoading} className='form-btn'>
                    {isLoading ?(
                        <>
                            <Icon
                                icon={Loading03Icon}
                                size={20}
                                className='animate-spin'
                            /> &nbsp; Loading...
                        </>
                    ): type === 'sign-in'
                    ? 'Sign In' : 'Sign Up'}
                </Button>

                </div>
            </form>
        </Form>

        <footer className="flex justify-center gap-1">
            <p className='text1-4 font-normal text-gray-600'>
                {type ==='sign-in'
               ? 'Don\'t have an account?'
               : 'Already have an account?'}
            </p>
            <Link href={type==='sign-in'?'/sign-up':'/sign-in'} className='form-link'>
                {type==='sign-in'?'Sign Up':'Sign in'}
            </Link>
        </footer>
    </section>
)}
export default AuthForm
