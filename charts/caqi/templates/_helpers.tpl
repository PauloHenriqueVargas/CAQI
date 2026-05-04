{{/*
Nome curto e completo (truncados para limite k8s de 63 chars).
*/}}
{{- define "caqi.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- define "caqi.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/* Labels comuns aplicadas a todos os recursos */}}
{{- define "caqi.labels" -}}
helm.sh/chart: {{ printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" }}
app.kubernetes.io/name: {{ include "caqi.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
caqi.gov.br/tenant: {{ .Values.tenant.municipioId | quote }}
caqi.gov.br/municipio: {{ .Values.tenant.municipioNome | quote }}
{{- end -}}

{{- define "caqi.selectorLabels" -}}
app.kubernetes.io/name: {{ include "caqi.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end -}}

{{/* Nome de service por microsserviço */}}
{{- define "caqi.svcName" -}}
{{- printf "%s-%s" (include "caqi.fullname" .root) .svcKey -}}
{{- end -}}
