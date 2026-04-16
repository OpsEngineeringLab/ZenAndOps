import type { SliSloCompliance } from "../../types/dashboard";

interface SliSloComplianceCardProps {
  data: SliSloCompliance;
}

function ComplianceRow({
  label,
  sli,
  slo,
}: {
  label: string;
  sli: number;
  slo: number;
}) {
  const isCompliant = sli >= slo;
  const percentage = Math.min((sli / slo) * 100, 100);

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <span className="text-sm font-medium text-gray-700 dark:text-gray-300">
          {label}
        </span>
        <span
          className={`text-sm font-semibold ${
            isCompliant
              ? "text-success-600 dark:text-success-400"
              : "text-error-600 dark:text-error-400"
          }`}
        >
          {sli}% / {slo}%
        </span>
      </div>
      <div className="w-full h-2.5 rounded-full bg-gray-200 dark:bg-gray-700">
        <div
          className={`h-2.5 rounded-full transition-all ${
            isCompliant ? "bg-success-500" : "bg-error-500"
          }`}
          style={{ width: `${percentage}%` }}
        />
      </div>
      <div className="flex justify-between text-xs text-gray-500 dark:text-gray-400">
        <span>SLI: {sli}%</span>
        <span>SLO Target: {slo}%</span>
      </div>
    </div>
  );
}

export default function SliSloComplianceCard({ data }: SliSloComplianceCardProps) {
  return (
    <div className="rounded-2xl border border-gray-200 bg-white p-5 dark:border-gray-800 dark:bg-white/[0.03] md:p-6">
      <h3 className="text-lg font-semibold text-gray-800 dark:text-white/90 mb-6">
        SLI/SLO Compliance
      </h3>
      <div className="space-y-6">
        <ComplianceRow
          label="Availability"
          sli={data.availabilitySli}
          slo={data.availabilitySlo}
        />
        <ComplianceRow
          label="Latency"
          sli={data.latencySli}
          slo={data.latencySlo}
        />
      </div>
    </div>
  );
}
