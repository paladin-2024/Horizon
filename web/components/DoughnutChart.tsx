"use client"
import { Pie, PieChart, Cell } from "recharts"
import { ChartContainer, ChartTooltip, ChartTooltipContent, type ChartConfig } from "./ui/chart"
import { formatMoney } from "@/lib/money"

const PALETTE = {
    light: ["#0179FE", "#2265d8", "#2f91fa", "#4893FF", "#194185", "#6172F3"],
    // White-on-blue tints for the gradient hero card — the light palette is invisible there.
    dark: ["#FFFFFF", "rgba(255,255,255,0.75)", "rgba(255,255,255,0.55)", "rgba(255,255,255,0.4)"],
};

/** Renders accounts that already share one currency — callers must group by currency first. */
const DoughnutChart = ({ accounts, tone = "light" }: { accounts: ApiAccount[]; tone?: "light" | "dark" }) => {
    const hasAccounts = accounts.length > 0;
    const currency = accounts[0]?.balance.currency ?? "";
    const emptyColor = tone === "dark" ? "rgba(255,255,255,0.25)" : "#EAECF0";
    const palette = PALETTE[tone];

    const data = hasAccounts
        ? accounts.map((a, i) => ({
              name: a.displayName,
              value: a.balance.amountMinor,
              fill: palette[i % palette.length],
          }))
        : [{ name: "No accounts linked", value: 1, fill: emptyColor }];

    const config: ChartConfig = Object.fromEntries(data.map((d) => [d.name, { label: d.name }]));

    return (
        <ChartContainer config={config} className="aspect-square h-full w-full">
            <PieChart>
                {hasAccounts && (
                    <ChartTooltip
                        content={
                            <ChartTooltipContent
                                hideLabel
                                formatter={(value, name) => (
                                    <div className="flex w-full items-center justify-between gap-4">
                                        <span className="text-muted-foreground">{name}</span>
                                        <span className="font-medium text-foreground">
                                            {formatMoney(Number(value), currency)}
                                        </span>
                                    </div>
                                )}
                            />
                        }
                    />
                )}
                <Pie
                    data={data}
                    dataKey="value"
                    nameKey="name"
                    innerRadius="72%"
                    strokeWidth={0}
                    startAngle={90}
                    endAngle={-270}
                >
                    {data.map((entry) => (
                        <Cell key={entry.name} fill={entry.fill} />
                    ))}
                </Pie>
            </PieChart>
        </ChartContainer>
    );
};

export default DoughnutChart;
