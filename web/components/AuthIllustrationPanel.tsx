import Image from "next/image";

interface AuthIllustrationPanelProps {
  illustration: string;
  headline: string;
  tagline: string;
}

/**
 * The brand side of the sign-in / sign-up screen. Hidden below `lg`, since the
 * form is the actual job on a phone and the illustration is decoration.
 *
 * True `fixed` positioning, not `sticky`: sticky still shifts right at the top
 * and bottom edge of its containing block on a long scrolling form, which read
 * as jittery. Fixed pins it to the viewport with zero movement, ever. Because
 * fixed elements leave the flex flow, the caller must reserve matching space
 * (see the `lg:w-1/2` on the form column in sign-in/page.tsx and sign-up/page.tsx).
 */
const AuthIllustrationPanel = ({ illustration, headline, tagline }: AuthIllustrationPanelProps) => {
  return (
    <div className="relative hidden flex-col items-center justify-center gap-8 overflow-hidden bg-sky-1 p-12 lg:fixed lg:right-0 lg:top-0 lg:flex lg:h-screen lg:w-1/2">
      <div className="pointer-events-none absolute -right-24 -top-24 size-96 rounded-full bg-bankGradient/25 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-32 -left-16 size-80 rounded-full bg-pink-500/20 blur-3xl" />
      <div className="pointer-events-none absolute left-1/3 top-1/4 size-64 rounded-full bg-indigo-500/15 blur-3xl" />

      <Image
        src={illustration}
        alt=""
        width={420}
        height={310}
        className="auth-illustration relative z-10 h-auto w-full max-w-[420px] drop-shadow-xl"
      />
      <div className="relative z-10 max-w-md text-center">
        <h2 className="text-20 font-semibold text-gray-900">{headline}</h2>
        <p className="text-14 mt-2 text-gray-600">{tagline}</p>
      </div>
    </div>
  );
};

export default AuthIllustrationPanel;
