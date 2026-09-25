import Image from 'next/image'
import Sidebar from "@/components/Sidebar";
import MobileNav from '@/components/MobileNav';
import { mockCurrentUser } from '@/lib/session';
export default function RootLayout({
children,
}: Readonly<{
children: React.ReactNode;
}>) {
    const loggedIn = mockCurrentUser;

return (
    <main className="flex h-screen w-full font-sans ">
        <Sidebar user={loggedIn}/>
        
        <div className="flex size-full flex-col">
            <div className="root-layout">
                <Image src="/icons/logo.svg"
                width={30}
                height={30}
                alt="logo" />
                <div>
                    <MobileNav user={loggedIn}/>
                </div>
            </div>
            {children}
        </div>
        
    </main>
);
}
