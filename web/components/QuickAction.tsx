import Link from "next/link";
import { IconSvgElement } from "@hugeicons/react";
import Icon from "./Icon";

/** Icon-in-a-tinted-circle action tile, the same idiom as StatCard's badge — see DESIGN.md. */
const QuickAction = ({ icon, label, href }: { icon: IconSvgElement; label: string; href: string }) => (
  <Link href={href} className="flex flex-col items-center gap-2 text-center">
    <span className="flex size-12 items-center justify-center rounded-full bg-blue-25 text-blue-700 transition-colors hover:bg-blue-100">
      <Icon icon={icon} size={22} />
    </span>
    <p className="text-12 font-medium text-gray-700">{label}</p>
  </Link>
);

export default QuickAction;
