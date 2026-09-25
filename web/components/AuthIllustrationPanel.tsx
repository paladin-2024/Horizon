import Image from "next/image";

interface AuthIllustrationPanelProps {
  illustration: string;
  headline: string;
  tagline: string;
}

/**
 * The brand side of the sign-in / sign-up screen. Hidden below `lg`, since the
 * form is the actual job on a phone and the illustration is decoration.
 */
const AuthIllustrationPanel = ({ illustration, headline, tagline }: AuthIllustrationPanelProps) => {
  return (
    <div className="hidden flex-1 flex-col items-center justify-center gap-8 bg-sky-1 p-12 lg:flex">
      <Image src={illustration} alt="" width={420} height={310} className="h-auto w-full max-w-[420px]" />
      <div className="max-w-md text-center">
        <h2 className="text-20 font-semibold text-gray-900">{headline}</h2>
        <p className="text-14 mt-2 text-gray-600">{tagline}</p>
      </div>
    </div>
  );
};

export default AuthIllustrationPanel;
