import SessionRefresh from '@/components/SessionRefresh'
import { safeNextPath } from '@/lib/session'

async function RefreshSession({
  searchParams,
}: {
  searchParams: Promise<{ next?: string | string[] }>
}) {
  const { next } = await searchParams
  return <SessionRefresh next={safeNextPath(Array.isArray(next) ? next[0] : next)} />
}

export default RefreshSession
