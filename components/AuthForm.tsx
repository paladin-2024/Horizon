'use client';
import Link from 'next/link'
import Image from 'next/image'
import { useState } from 'react'
import { z } from "zod"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import { Button } from "@/components/ui/button"
import {
  Form,
  FormControl,
  FormField,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import CustomInput from './CustomInput';
import { authFormSchema } from '@/lib/utils';



const AuthForm = ({ type }:{ type: string}) => {
    const [user, setUser] = useState(null)

    const form = useForm<z.infer<typeof authFormSchema>>({
        resolver: zodResolver(authFormSchema),
        defaultValues: {
            email: "",
            password: "",
        },
    })

    function onSubmit(values: z.infer<typeof authFormSchema>) {
        
        console.log(values)
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
                <h1 className='text-26 font-ibm-plex-serif font-bold text-black-1'>Horizon</h1>
            </Link>
            <div className="flex flex-col gap-1 md:gap-3">
                <h1 className='text-24 lg:text-36 font-semibold text-gray-900'>
                    {user
                    ?'Link Account'
                    : type === 'sign-in'
                        ? 'Sign In'
                        : 'Sign Up'
                    }
                </h1>

                <p className="text-16 font-normal text-gray-600">
                    {user
                        ? 'Link your account to get started'
                        : 'Please enter your details'
                    }
                </p>
            </div>
        </header>

        {user?(
            <div className="flex flex-col gap-4">
                {/*Plaid Link*/}
            </div>
        ):(
            <>
                <Form {...form}>
                    <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-8">
                        <FormField
                        control={form.control}
                        name="email"
                        render={({ field }) => (
                            <div className="form-item">
                                <FormLabel className='form-label'>
                                    Email
                                </FormLabel>
                                <div className="flex w-full flex-col">
                                    <FormControl>
                                        <Input 
                                        placeholder='Enter your email'
                                        className='input-class'
                                        {...field}
                                        />
                                    </FormControl>
                                    <FormMessage className="form-message mt-2" />
                                </div>
                            </div>
                        )}
                        />

                        <CustomInput 
                        control={form.control}
                        name='username'
                        label='Username'
                        placeholder='Enter your username'
                        />

                        <CustomInput 
                        control={form.control}
                        name='password'
                        label='Password'
                        placeholder='Enter your password'
                        />

                        <FormField
                            control={form.control}
                            name="password"
                            render={({ field }) => (
                                <div className="form-item">
                                    <FormLabel className='form-label'>
                                        Password
                                    </FormLabel>
                                    <div className="flex w-full flex-col">
                                        <FormControl>
                                            <Input 
                                            placeholder='Enter your password'
                                            className='input-class'
                                            type='password'
                                            {...field}
                                            />
                                        </FormControl>
                                        <FormMessage className="form-message mt-2" />
                                    </div>
                                </div>
                            )}
                        />
                        <Button type="submit">Submit</Button>
                    </form>
                </Form>
  
            </>
        )
    }
    </section>
)}
export default AuthForm
