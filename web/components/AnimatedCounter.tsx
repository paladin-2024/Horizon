'use client'
import CountUp from "react-countup"
import { toDecimal } from "@/lib/money"

const AnimatedCounter = ({ amountMinor, currency }: { amountMinor: number; currency: string }) => {
    return (
        <div className="flex items-baseline gap-2">
            <CountUp
                decimals={2}
                decimal="."
                separator=","
                end={toDecimal(amountMinor)}
            />
            <span className="text-14 font-semibold text-gray-500">{currency}</span>
        </div>
    )
}

export default AnimatedCounter
