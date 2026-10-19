import { cookies } from 'next/headers'
import HeaderBox from '@/components/HeaderBox'
import { Card } from '@/components/ui/card'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import Icon from '@/components/Icon'
import { cookieHeader, fetchMe } from '@/lib/api/server'
import Mail01Icon from '@hugeicons/core-free-icons/Mail01Icon'
import SmartPhone01Icon from '@hugeicons/core-free-icons/SmartPhone01Icon'
import Location01Icon from '@hugeicons/core-free-icons/Location01Icon'
import LanguageSquareIcon from '@hugeicons/core-free-icons/LanguageSquareIcon'

const COUNTRY_NAME: Record<string, string> = { UG: 'Uganda', CD: 'DR Congo' }
const LANGUAGE_NAME: Record<string, string> = { en: 'English', fr: 'Français' }

const Profile = async () => {
  const header = cookieHeader((await cookies()).getAll())
  const user = await fetchMe(header)
  if (!user) return null

  return (
    <div className="my-banks">
      <HeaderBox title="Profile" subtext="Your identity on Horizon." />

      <Card className="flex max-w-md flex-col items-start gap-5 p-6">
        <Avatar className="size-16">
          <AvatarFallback className="bg-bank-gradient text-20 font-semibold text-white">
            {user.firstName[0]}
          </AvatarFallback>
        </Avatar>
        <div>
          <h2 className="text-18 font-semibold text-gray-900">
            {user.firstName} {user.lastName}
          </h2>
          <p className="text-14 text-gray-500">Horizon member</p>
        </div>

        <div className="flex w-full flex-col gap-3 border-t border-gray-200 pt-4">
          <div className="flex items-center gap-3">
            <Icon icon={SmartPhone01Icon} size={18} className="text-gray-500" />
            <p className="text-14 text-gray-700">{user.phone}</p>
          </div>
          {user.email && (
            <div className="flex items-center gap-3">
              <Icon icon={Mail01Icon} size={18} className="text-gray-500" />
              <p className="text-14 text-gray-700">{user.email}</p>
            </div>
          )}
          <div className="flex items-center gap-3">
            <Icon icon={Location01Icon} size={18} className="text-gray-500" />
            <p className="text-14 text-gray-700">{COUNTRY_NAME[user.country] ?? user.country}</p>
          </div>
          <div className="flex items-center gap-3">
            <Icon icon={LanguageSquareIcon} size={18} className="text-gray-500" />
            <p className="text-14 text-gray-700">{LANGUAGE_NAME[user.language] ?? user.language}</p>
          </div>
        </div>
      </Card>
    </div>
  )
}

export default Profile
