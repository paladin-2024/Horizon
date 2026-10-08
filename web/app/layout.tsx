import type { Metadata } from "next";
import localFont from "next/font/local";
import "./globals.css";

// Self-hosted: Switzer is a Fontshare family, not on next/font/google.
// See assets/fonts/switzer/LICENSE.md for Fontshare's free license terms.
const switzer = localFont({
  src: [
    { path: "../assets/fonts/switzer/Switzer-Regular.woff2", weight: "400", style: "normal" },
    { path: "../assets/fonts/switzer/Switzer-Medium.woff2", weight: "500", style: "normal" },
    { path: "../assets/fonts/switzer/Switzer-SemiBold.woff2", weight: "600", style: "normal" },
    { path: "../assets/fonts/switzer/Switzer-Bold.woff2", weight: "700", style: "normal" },
    { path: "../assets/fonts/switzer/Switzer-Extrabold.woff2", weight: "800", style: "normal" },
  ],
  variable: "--font-sans",
  display: "swap",
});


export const metadata: Metadata = {
  title: "Horizon",
  description: "Horizon is a modern banking platform for everyone ",
  icons:{
    icon:'/icons/logo.svg'
  }
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={switzer.variable}>
      <body>
        {children}
      </body>
    </html>
  );
}
