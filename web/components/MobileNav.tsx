'use client'
import{
    Sheet,
    SheetContent,
    SheetTrigger,
    SheetClose,
  } from "@/components/ui/sheet"
import { sidebarLinks } from "@/constants"
import { cn } from "@/lib/utils"
import Image from "next/image"
import Link from "next/link"
import { usePathname } from "next/navigation"
import Icon from "./Icon"
import MenuIcon from "@hugeicons/core-free-icons/Menu01Icon"
import { Avatar, AvatarFallback } from "./ui/avatar"

const MobileNav = ({user}: MobileNavProps) => {
    const pathname=usePathname();
  return (
    <section className="w-full max-w-[264px">
        <Sheet>

            <SheetTrigger>
                <Icon icon={MenuIcon} size={28} className="cursor-pointer text-black-2" />
            </SheetTrigger>

            <SheetContent side="left" className="bordern-one bg-white">

            <Link  href="/" className='cursor-pointer flex items-center gap-1 px-4'>
            <Image
                src="/icons/logo.svg"
                width={34}
                height={34}
                alt='Horizon logo'
            />
            <h1 className='text-26 font-sans font-bold text-black-1'>Horizon</h1>
            </Link>

            <div className="mobilenav-sheet">
                <SheetClose asChild>
                    <nav className="flex h-full flex-col gap-6 pt-16 text-white">
                        {sidebarLinks.map((item)=>{
                            const isActive = pathname === item.route || pathname.startsWith(`${item.route}/`)
                            return(
                            <SheetClose asChild key={item.route}>
                                <Link href={item.route} key={item.label}
                                    className={cn
                                    ('mobilenav-sheet_close w-full',{
                                        'bg-bank-gradient':isActive
                                    })}>

                                    <Icon
                                        icon={item.icon}
                                        size={20}
                                        className={cn({'text-white': isActive})}
                                    />
                                    <p className={cn('text-16 font-semibold text-black-2',{
                                        'text-white':isActive
                                    })}>
                                    {item.label}
                                    </p>
                                </Link>
                            </SheetClose>
                        )})}

                        <div className="flex items-center gap-3 border-t border-gray-200 pt-4">
                            <Avatar className="size-10 shrink-0">
                                <AvatarFallback className="bg-bank-gradient text-white font-semibold">
                                    {user.firstName?.[0]}
                                </AvatarFallback>
                            </Avatar>
                            <div className="min-w-0">
                                <p className="text-14 font-semibold text-black-2 truncate">
                                    {user.firstName} {user.lastName}
                                </p>
                                <p className="text-12 font-normal text-gray-500 truncate">{user.email}</p>
                            </div>
                        </div>
                    </nav>
                </SheetClose>
            </div>
            </SheetContent>
        </Sheet>
    </section>
)}

export default MobileNav
