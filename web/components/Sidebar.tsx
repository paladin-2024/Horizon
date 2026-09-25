'use client'
import { sidebarLinks } from '@/constants'
import Image from 'next/image'
import Link from 'next/link'
import {cn} from '@/lib/utils'
import {usePathname} from 'next/navigation'
import Icon from './Icon'
import { Avatar, AvatarFallback } from './ui/avatar'

const Sidebar = ({user }:SiderbarProps) => {
    const pathname=usePathname();
    return (
    <section className='sidebar'>
        <nav className='flex flex-col gap-4'>
            <Link  href="/" className='mb-12 cursor-pointer flex items-center gap-2'>
            <Image
                src="/icons/logo.svg"
                width={34}
                height={34}
                alt='Horizon logo'
                className="size-[24px]
                max-lg:size-14"
            />
            <h1 className='sidebar-logo'>Horizon</h1>
            </Link>

            {sidebarLinks.map((item)=>{
                const isActive = pathname === item.route || pathname.startsWith(`${item.route}/`)
            return(
                <Link href={item.route} key={item.label}
                className={cn
                    ('sidebar-link',{
                        'bg-bank-gradient':isActive
                    })}
                >
                    <Icon
                        icon={item.icon}
                        size={22}
                        className={cn('text-black-2', {'text-white': isActive})}
                    />
                    <p className={cn('sidebar-label',{
                        '!text-white':isActive
                    })}>
                        {item.label}
                    </p>
                </Link>
            )})}

        </nav>
        <div className="flex items-center justify-center gap-3 border-t border-gray-200 pt-4 lg:justify-start">
            <Avatar className="size-10 shrink-0">
                <AvatarFallback className="bg-bank-gradient text-white font-semibold">
                    {user.firstName?.[0]}
                </AvatarFallback>
            </Avatar>
            <div className="footer_email min-w-0">
                <p className="text-14 font-semibold text-black-2 truncate">
                    {user.firstName} {user.lastName}
                </p>
                <p className="text-12 font-normal text-gray-500 truncate">{user.email}</p>
            </div>
        </div>
    </section>
    )
}

export default Sidebar
