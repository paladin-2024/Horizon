import { HugeiconsIcon, IconSvgElement } from "@hugeicons/react";
import { cn } from "@/lib/utils";

interface IconProps {
  icon: IconSvgElement;
  size?: number;
  className?: string;
  strokeWidth?: number;
}

/**
 * The single way functional UI icons render in this app. Every icon inherits
 * text color via `currentColor` (stroke), so recoloring is a className, not a prop.
 */
const Icon = ({ icon, size = 20, className, strokeWidth = 1.5 }: IconProps) => (
  <HugeiconsIcon
    icon={icon}
    size={size}
    strokeWidth={strokeWidth}
    className={cn("shrink-0", className)}
  />
);

export default Icon;
