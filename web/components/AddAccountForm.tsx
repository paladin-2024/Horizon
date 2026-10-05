'use client'
import { useMemo, useState } from 'react'
import { z } from 'zod'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useRouter } from 'next/navigation'
import {
  Sheet,
  SheetContent,
  SheetTrigger,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from '@/components/ui/sheet'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Form, FormControl, FormField, FormLabel, FormMessage } from '@/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { linkAccountFormSchema } from '@/lib/utils'
import { createAccount } from '@/lib/api/accounts'
import { errorMessage } from '@/lib/api/messages'
import { toMinorUnits } from '@/lib/money'
import Icon from './Icon'
import PlusSignIcon from '@hugeicons/core-free-icons/PlusSignIcon'
import Loading03Icon from '@hugeicons/core-free-icons/Loading03Icon'

type LinkAccountFormValues = z.infer<typeof linkAccountFormSchema>

const CURRENCY_LABEL: Record<string, string> = {
  UGX: 'UGX — Ugandan Shilling',
  CDF: 'CDF — Congolese Franc',
  USD: 'USD — US Dollar',
};

const COUNTRY_LABEL: Record<InstitutionCountry, string> = {
  UG: 'Uganda',
  CD: 'DR Congo',
};

const AddAccountForm = ({ institutions }: { institutions: ApiInstitution[] }) => {
  const router = useRouter()
  const [open, setOpen] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [formError, setFormError] = useState<string | null>(null)

  const grouped = useMemo(() => {
    const byCountry = new Map<InstitutionCountry, ApiInstitution[]>()
    for (const inst of institutions) {
      const list = byCountry.get(inst.country) ?? []
      list.push(inst)
      byCountry.set(inst.country, list)
    }
    return byCountry
  }, [institutions])

  const form = useForm<LinkAccountFormValues>({
    resolver: zodResolver(linkAccountFormSchema),
    defaultValues: {
      institutionId: '',
      provider: 'MANUAL',
      displayName: '',
      accountMask: '',
      currency: 'UGX',
      openingBalance: '',
    },
  })

  const onSubmit = async (data: LinkAccountFormValues) => {
    setIsLoading(true)
    setFormError(null)
    try {
      await createAccount({
        institutionId: data.institutionId,
        provider: 'MANUAL',
        displayName: data.displayName,
        accountMask: data.accountMask,
        currency: data.currency,
        openingBalanceMinor: toMinorUnits(Number(data.openingBalance)),
      })
      setOpen(false)
      form.reset()
      router.refresh()
    } catch (error) {
      setFormError(
        errorMessage(error, "We couldn't link that account. Check the details and try again.")
      )
    } finally {
      setIsLoading(false)
    }
  }

  return (
    <Sheet open={open} onOpenChange={setOpen}>
      <SheetTrigger asChild>
        <Button className="form-btn gap-2">
          <Icon icon={PlusSignIcon} size={18} className="text-white" />
          Link an account
        </Button>
      </SheetTrigger>

      <SheetContent side="right" className="w-full overflow-y-auto sm:max-w-md">
        <SheetHeader>
          <SheetTitle>Link an account</SheetTitle>
          <SheetDescription>
            Add a bank or mobile money account manually. You can update the balance any time.
          </SheetDescription>
        </SheetHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="mt-6 space-y-4">
            <FormField
              control={form.control}
              name="institutionId"
              render={({ field }) => (
                <div className="form-item">
                  <FormLabel className="form-label">Institution</FormLabel>
                  <FormControl>
                    <Select onValueChange={field.onChange} value={field.value}>
                      <SelectTrigger className="input-class">
                        <SelectValue placeholder="Choose a bank or mobile money wallet" />
                      </SelectTrigger>
                      <SelectContent>
                        {(['UG', 'CD'] as InstitutionCountry[]).map((country) =>
                          grouped.get(country)?.length ? (
                            <div key={country}>
                              <p className="px-2 pb-1 pt-2 text-12 font-semibold uppercase tracking-wide text-gray-500">
                                {COUNTRY_LABEL[country]}
                              </p>
                              {grouped.get(country)!.map((inst) => (
                                <SelectItem key={inst.id} value={inst.id}>
                                  {inst.name}
                                </SelectItem>
                              ))}
                            </div>
                          ) : null
                        )}
                      </SelectContent>
                    </Select>
                  </FormControl>
                  <FormMessage className="form-message mt-2" />
                </div>
              )}
            />

            <FormField
              control={form.control}
              name="displayName"
              render={({ field }) => (
                <div className="form-item">
                  <FormLabel className="form-label">Account name</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g. Salary account" className="input-class" {...field} />
                  </FormControl>
                  <FormMessage className="form-message mt-2" />
                </div>
              )}
            />

            <div className="flex gap-3">
              <FormField
                control={form.control}
                name="accountMask"
                render={({ field }) => (
                  <div className="form-item flex-1">
                    <FormLabel className="form-label">Last 4 digits</FormLabel>
                    <FormControl>
                      <Input
                        placeholder="4821"
                        inputMode="numeric"
                        maxLength={4}
                        className="input-class"
                        {...field}
                      />
                    </FormControl>
                    <FormMessage className="form-message mt-2" />
                  </div>
                )}
              />

              <FormField
                control={form.control}
                name="currency"
                render={({ field }) => (
                  <div className="form-item flex-1">
                    <FormLabel className="form-label">Currency</FormLabel>
                    <FormControl>
                      <Select onValueChange={field.onChange} value={field.value}>
                        <SelectTrigger className="input-class">
                          <SelectValue />
                        </SelectTrigger>
                        <SelectContent>
                          {Object.entries(CURRENCY_LABEL).map(([code, label]) => (
                            <SelectItem key={code} value={code}>
                              {label}
                            </SelectItem>
                          ))}
                        </SelectContent>
                      </Select>
                    </FormControl>
                    <FormMessage className="form-message mt-2" />
                  </div>
                )}
              />
            </div>

            <FormField
              control={form.control}
              name="openingBalance"
              render={({ field }) => (
                <div className="form-item">
                  <FormLabel className="form-label">Current balance</FormLabel>
                  <FormControl>
                    <Input
                      placeholder="0.00"
                      inputMode="decimal"
                      className="input-class"
                      {...field}
                    />
                  </FormControl>
                  <FormMessage className="form-message mt-2" />
                </div>
              )}
            />

            {formError && (
              <p className="form-message" role="alert">
                {formError}
              </p>
            )}

            <Button type="submit" disabled={isLoading} className="form-btn w-full">
              {isLoading ? (
                <>
                  <Icon icon={Loading03Icon} size={20} className="animate-spin" /> &nbsp; Linking...
                </>
              ) : (
                'Link account'
              )}
            </Button>
          </form>
        </Form>
      </SheetContent>
    </Sheet>
  )
}

export default AddAccountForm
