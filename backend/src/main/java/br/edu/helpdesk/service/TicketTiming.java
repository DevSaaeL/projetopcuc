package br.edu.helpdesk.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/** One response budget from opening, followed by a resolution budget from first pickup. */
public final class TicketTiming {
 private TicketTiming() {}
 public static void decorate(Map<String,Object> ticket, Instant now) {
  Instant opened=Instant.parse(ticket.get("abertura").toString());
  Instant taken=ticket.get("atendimento")==null?null:Instant.parse(ticket.get("atendimento").toString());
  Instant finished=ticket.get("finalizacao")==null?null:Instant.parse(ticket.get("finalizacao").toString());
  Instant end=finished==null?now:finished;
  long response=seconds(opened,taken==null?end:taken);
  long resolution=taken==null?0:seconds(taken,end);
  long responseBudget=((Number)ticket.get("sla_resposta")).longValue()*60;
  long resolutionBudget=((Number)ticket.get("sla_resolucao")).longValue()*60;
  ticket.put("servidor_agora",now.toString());
  ticket.put("tempo_resposta_segundos",response);
  ticket.put("tempo_atendimento_segundos",resolution);
  ticket.put("tempo_total_segundos",seconds(opened,end));
  ticket.put("resposta_restante_segundos",responseBudget-response);
  ticket.put("resolucao_restante_segundos",resolutionBudget-resolution);
  ticket.put("prazo_assumir",opened.plusSeconds(responseBudget).toString());
  ticket.put("prazo_finalizar",taken==null?null:taken.plusSeconds(resolutionBudget).toString());
  ticket.put("fora_sla",!"CANCELADO".equals(ticket.get("status"))&&(response>responseBudget||resolution>resolutionBudget));
 }
 private static long seconds(Instant start,Instant end){return Math.max(0,Duration.between(start,end).toSeconds());}
}
