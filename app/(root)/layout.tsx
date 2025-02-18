import Image from 'next/image'
import Sidebar from "@/components/Sidebar";
export default function RootLayout({
children,
}: Readonly<{
children: React.ReactNode;
}>) {
    const loggedIn = {firstName:'Paladin', lastName:'Caleb'};

return (
    <main className="flex h-screen w-full font-inter ">
        <Sidebar user={loggedIn}/>
        
        <div className="flex size-full flex-col">
            <div className="root-layout">
                <Image src="/icons/logo.svg"
                width={30}
                height={30}
                alt="menu icon" />
                    <div>
                        
                    </div>
            </div>
        </div>
        {children}
    </main>
);
}
