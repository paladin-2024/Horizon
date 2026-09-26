import Image from 'next/image'
import { cookies, headers } from 'next/headers'
import { redirect } from 'next/navigation'
import Sidebar from "@/components/Sidebar";
import MobileNav from '@/components/MobileNav';
import { cookieHeader, fetchMe } from '@/lib/api/server';
import { GATE_COOKIE, PATH_HEADER, unauthenticatedRedirect } from '@/lib/session';

export default async function RootLayout({
children,
}: Readonly<{
children: React.ReactNode;
}>) {
    const cookieStore = await cookies();
    const loggedIn = await fetchMe(cookieHeader(cookieStore.getAll()));
    if (!loggedIn) {
        const path = (await headers()).get(PATH_HEADER) ?? '/';
        redirect(unauthenticatedRedirect(path, cookieStore.has(GATE_COOKIE)));
    }

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
