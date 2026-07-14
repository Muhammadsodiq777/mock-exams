import { useMemo, useState, type FormEvent } from 'react'

import {
  Alert,
  Badge,
  Button,
  DescriptionList,
  EmptyState,
  Loader,
  PageContainer,
  PageHeader,
  Panel,
  RepeatedItemCard,
  SelectField,
  TextField,
} from '../../components/ui'
import { useAdminSettingUpdate, useAdminSettings } from '../../hooks/useAdminSettings'
import type { PlatformSettingRDO, PlatformSettingValue } from '../../types/backend'
import { normalizeApiError } from '../../utils/apiErrors'
import { formatDate, humanizeToken } from '../../utils/formatters'

type SettingField = {
  helpText?: string
  key: string
  label: string
  type: 'array' | 'boolean' | 'number' | 'string'
}

type SettingDescriptor = {
  fields: SettingField[]
  group: string
  key: string
  title: string
}

const descriptors: SettingDescriptor[] = [
  {
    fields: [{ key: 'value', label: 'Commission percent', type: 'number' }],
    group: 'Marketplace',
    key: 'marketplace.default_commission_percent',
    title: 'Marketplace commission',
  },
  {
    fields: [{ key: 'value', label: 'Commission percent', type: 'number' }],
    group: 'Platform',
    key: 'platform.default_commission_percent',
    title: 'Platform commission',
  },
  {
    fields: [{ key: 'value', label: 'Commission percent', type: 'number' }],
    group: 'Live speaking',
    key: 'live_speaking.platform_commission_percent',
    title: 'Live speaking commission',
  },
  {
    fields: [
      { key: 'value', label: 'Free requests', type: 'number' },
      { key: 'period', label: 'Period', type: 'string' },
    ],
    group: 'AI',
    key: 'ai.free_monthly_requests',
    title: 'Free AI allowance',
  },
  {
    fields: [
      { key: 'value', label: 'Hard cap requests', type: 'number' },
      { key: 'period', label: 'Period', type: 'string' },
    ],
    group: 'AI',
    key: 'ai.hard_monthly_requests',
    title: 'AI hard cap',
  },
  {
    fields: [
      { key: 'provider', label: 'Provider', type: 'string' },
      { key: 'model', label: 'Model', type: 'string' },
    ],
    group: 'AI',
    key: 'ai.default_model',
    title: 'AI default model',
  },
  {
    fields: [
      { key: 'writingRubricVersion', label: 'Writing rubric version', type: 'string' },
      { key: 'speakingRubricVersion', label: 'Speaking rubric version', type: 'string' },
    ],
    group: 'AI',
    key: 'ai.rubric_defaults',
    title: 'AI rubric defaults',
  },
  {
    fields: [
      { key: 'amount', label: 'Amount', type: 'number' },
      { key: 'currency', label: 'Currency', type: 'string' },
    ],
    group: 'Pricing',
    key: 'ai.premium_monthly_price',
    title: 'Premium monthly price',
  },
  {
    fields: [
      { key: 'enabled', label: 'Provider enabled', type: 'boolean' },
      { key: 'provider', label: 'Provider', type: 'string' },
      { key: 'mode', label: 'Mode', type: 'string' },
    ],
    group: 'Payments',
    key: 'payments.provider_mode',
    title: 'Payment provider mode',
  },
  {
    fields: [
      { key: 'defaultProvider', label: 'Default provider', type: 'string' },
      { key: 'allowedProviders', label: 'Allowed providers', type: 'array' },
    ],
    group: 'Payments',
    key: 'payments.provider_options',
    title: 'Payment provider options',
  },
  {
    fields: [{ key: 'value', label: 'Currency', type: 'string' }],
    group: 'Payments',
    key: 'payments.default_currency',
    title: 'Default payment currency',
  },
  {
    fields: [
      { key: 'value', label: 'Minimum amount', type: 'number' },
      { key: 'currency', label: 'Currency', type: 'string' },
    ],
    group: 'Wallet',
    key: 'wallet.minimum_withdrawal_amount',
    title: 'Minimum withdrawal',
  },
  {
    fields: [
      { key: 'autoHideThreshold', label: 'Auto-hide threshold', type: 'number' },
      { key: 'manualReviewRequired', label: 'Manual review required', type: 'boolean' },
    ],
    group: 'Moderation',
    key: 'moderation.behavior',
    title: 'Moderation behavior',
  },
  {
    fields: [
      { key: 'maxDocumentBytes', label: 'Max document bytes', type: 'number' },
      {
        helpText: 'Comma-separated file purposes allowed in rich-text embeds.',
        key: 'allowedEmbedPurposes',
        label: 'Allowed embed purposes',
        type: 'array',
      },
    ],
    group: 'Rich text',
    key: 'rich_text.limits',
    title: 'Rich-text limits',
  },
  {
    fields: [
      { key: 'enabled', label: 'Enabled', type: 'boolean' },
      { key: 'mode', label: 'Mode', type: 'string' },
      { key: 'calendarOwner', label: 'Calendar owner', type: 'string' },
    ],
    group: 'Google',
    key: 'google.calendar_meet',
    title: 'Calendar and Meet mode',
  },
  {
    fields: [
      {
        key: 'studentCancellationWindowHours',
        label: 'Student cancellation window hours',
        type: 'number',
      },
      {
        key: 'teacherCancellationWindowHours',
        label: 'Teacher cancellation window hours',
        type: 'number',
      },
    ],
    group: 'Live speaking',
    key: 'live_speaking.cancellation_policy',
    title: 'Cancellation policy',
  },
]

const descriptorsByKey = new Map(descriptors.map((descriptor) => [descriptor.key, descriptor]))

const fieldValue = (value: PlatformSettingValue, field: SettingField) => {
  const current = value[field.key]

  if (Array.isArray(current)) {
    return current.join(', ')
  }

  if (typeof current === 'boolean') {
    return current ? 'true' : 'false'
  }

  return current === undefined || current === null ? '' : String(current)
}

const coerceFieldValue = (value: string, field: SettingField) => {
  if (field.type === 'number') {
    const parsed = Number(value)
    return Number.isFinite(parsed) ? parsed : value
  }

  if (field.type === 'boolean') {
    return value === 'true'
  }

  if (field.type === 'array') {
    return value
      .split(',')
      .map((item) => item.trim())
      .filter(Boolean)
  }

  return value
}

const providerHealth = (setting: PlatformSettingRDO) => {
  const enabled = setting.value.enabled
  const mode = String(setting.value.mode ?? setting.value.provider ?? setting.value.defaultProvider ?? '')

  if (enabled === false || mode === 'disabled') {
    return { label: 'Disabled', tone: 'neutral' as const }
  }

  if (mode === 'mock') {
    return { label: 'Mock', tone: 'warning' as const }
  }

  return { label: 'Configured', tone: 'success' as const }
}

function SettingEditor({ setting }: { setting: PlatformSettingRDO }) {
  const descriptor = descriptorsByKey.get(setting.key)
  const settingUpdate = useAdminSettingUpdate()
  const [draft, setDraft] = useState<Record<string, string>>(() =>
    Object.fromEntries(
      (descriptor?.fields ?? []).map((field) => [field.key, fieldValue(setting.value, field)]),
    ),
  )
  const [savedMessage, setSavedMessage] = useState<string | null>(null)
  const isDirty = descriptor?.fields.some((field) => draft[field.key] !== fieldValue(setting.value, field))
  const error = settingUpdate.isError ? normalizeApiError(settingUpdate.error) : null

  if (!descriptor) {
    return (
      <RepeatedItemCard>
        <Badge tone="neutral">Unsupported</Badge>
        <h3>{humanizeToken(setting.key)}</h3>
        <p className="feature-card__summary">
          This setting is returned by the backend but does not have a typed editor yet.
        </p>
        <DescriptionList
          items={[
            { label: 'Key', value: setting.key },
            { label: 'Updated', value: formatDate(setting.updatedAt) },
          ]}
        />
      </RepeatedItemCard>
    )
  }

  const submit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    const value = Object.fromEntries(
      descriptor.fields.map((field) => [field.key, coerceFieldValue(draft[field.key] ?? '', field)]),
    )

    settingUpdate.mutate(
      { key: setting.key, payload: { value } },
      {
        onSuccess: () => setSavedMessage(`${descriptor.title} saved.`),
      },
    )
  }

  return (
    <RepeatedItemCard>
      <div className="feature-card__badges">
        <Badge tone="info">{descriptor.group}</Badge>
        {(setting.key.includes('provider') || setting.key.includes('google')) && (
          <Badge tone={providerHealth(setting).tone}>{providerHealth(setting).label}</Badge>
        )}
      </div>
      <h3>{descriptor.title}</h3>
      <DescriptionList
        items={[
          { label: 'Key', value: setting.key },
          { label: 'Updated', value: formatDate(setting.updatedAt) },
        ]}
      />
      {savedMessage && (
        <Alert title="Saved" variant="success">
          {savedMessage}
        </Alert>
      )}
      {error && (
        <Alert title="Setting could not be saved" variant="danger">
          {error.message}
        </Alert>
      )}
      <form className="feature-stack" onSubmit={submit}>
        {descriptor.fields.map((field) =>
          field.type === 'boolean' ? (
            <SelectField
              helpText={field.helpText}
              key={field.key}
              label={field.label}
              onChange={(event) => setDraft((current) => ({ ...current, [field.key]: event.target.value }))}
              value={draft[field.key] ?? 'false'}
            >
              <option value="false">False</option>
              <option value="true">True</option>
            </SelectField>
          ) : (
            <TextField
              helpText={field.helpText}
              key={field.key}
              label={field.label}
              onChange={(event) => setDraft((current) => ({ ...current, [field.key]: event.target.value }))}
              type={field.type === 'number' ? 'number' : 'text'}
              value={draft[field.key] ?? ''}
            />
          ),
        )}
        <div className="feature-card__actions">
          <Button disabled={!isDirty} isLoading={settingUpdate.isPending} loadingLabel="Saving" type="submit">
            Save
          </Button>
          <Button
            disabled={!isDirty || settingUpdate.isPending}
            onClick={() =>
              setDraft(
                Object.fromEntries(descriptor.fields.map((field) => [field.key, fieldValue(setting.value, field)])),
              )
            }
            variant="secondary"
          >
            Discard
          </Button>
        </div>
      </form>
    </RepeatedItemCard>
  )
}

export function AdminSettingsPage() {
  const settingsQuery = useAdminSettings()
  const settings = useMemo(() => settingsQuery.data?.items ?? [], [settingsQuery.data?.items])
  const groupedSettings = useMemo(
    () =>
      settings.reduce<Record<string, PlatformSettingRDO[]>>((groups, setting) => {
        const group = descriptorsByKey.get(setting.key)?.group ?? 'Other'
        groups[group] = [...(groups[group] ?? []), setting]
        return groups
      }, {}),
    [settings],
  )

  return (
    <PageContainer size="wide">
      <PageHeader eyebrow="Admin" title="Settings" />

      <Alert title="Secret safety" variant="info">
        Provider pages show mode and health only. Secret values stay server-side and are never
        rendered in these controls.
      </Alert>

      {settingsQuery.isLoading && (
        <Panel>
          <div className="feature-panel-state">
            <Loader label="Loading settings" />
          </div>
        </Panel>
      )}

      {settingsQuery.isError && (
        <Alert title="Settings could not be loaded" variant="danger">
          {normalizeApiError(settingsQuery.error).message}
        </Alert>
      )}

      {!settingsQuery.isLoading && !settingsQuery.isError && settings.length === 0 && (
        <Panel>
          <div className="feature-panel-state">
            <EmptyState title="No settings" message="The backend returned no platform settings." />
          </div>
        </Panel>
      )}

      {Object.entries(groupedSettings).map(([group, groupSettings]) => (
        <Panel key={group} title={group}>
          <div className="feature-card-grid feature-card-grid--compact">
            {groupSettings.map((setting) => (
              <SettingEditor key={setting.key} setting={setting} />
            ))}
          </div>
        </Panel>
      ))}
    </PageContainer>
  )
}
