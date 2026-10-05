import React, { useState } from 'react'
import { FormField,FormLabel,FormControl,FormMessage } from './ui/form'
import { Input } from './ui/input'
import { Control, FieldPath } from 'react-hook-form'
import { z } from 'zod'
import { authFormSchema } from '@/lib/utils'
import Icon from './Icon'
import ViewIcon from '@hugeicons/core-free-icons/ViewIcon'
import ViewOffIcon from '@hugeicons/core-free-icons/ViewOffIcon'


type FormSchema = ReturnType<typeof authFormSchema>

interface CustomInput{
    control: Control<z.infer<FormSchema>>,
    name: FieldPath<z.infer<FormSchema>>,
    label: string,
    placeholder: string
}

const CustomInput = ({control,name,label,placeholder}: CustomInput) => {
    const isPassword = name === "password"
    const [showPassword, setShowPassword] = useState(false)

    return (
        <FormField
            control={control}
            name={name}
            render={({ field }) => (
                <div className="form-item">
                    <FormLabel className='form-label'>
                        {label}
                    </FormLabel>
                    <div className="flex w-full flex-col">
                        <FormControl>
                            {isPassword ? (
                                <div className="relative flex items-center">
                                    <Input
                                    placeholder={placeholder}
                                    className='input-class pr-10'
                                    type={showPassword ? "text" : "password"}
                                    {...field}
                                    />
                                    <button
                                        type="button"
                                        onClick={() => setShowPassword((prev) => !prev)}
                                        className="absolute right-3 flex items-center text-gray-500 hover:text-gray-700"
                                        aria-label={showPassword ? "Hide password" : "Show password"}
                                        tabIndex={-1}
                                    >
                                        <Icon icon={showPassword ? ViewOffIcon : ViewIcon} size={18} />
                                    </button>
                                </div>
                            ) : (
                                <Input
                                placeholder={placeholder}
                                className='input-class'
                                type="text"
                                {...field}
                                />
                            )}
                        </FormControl>
                        <FormMessage className="form-message mt-2" />
                    </div>
                </div>
            )}
        />
    )
}

export default CustomInput
