import Icon from "./Icon";
import InformationCircleIcon from "@hugeicons/core-free-icons/InformationCircleIcon";

/** Required on any page reading from lib/sampleData.ts — see PRODUCT.md's "no fabricated data" principle. */
const SampleDataNotice = ({ children }: { children: React.ReactNode }) => (
  <div className="flex items-start gap-2 rounded-lg border border-blue-100 bg-blue-25 px-4 py-3">
    <Icon icon={InformationCircleIcon} size={18} className="mt-0.5 shrink-0 text-blue-700" />
    <p className="text-14 text-blue-900">{children}</p>
  </div>
);

export default SampleDataNotice;
