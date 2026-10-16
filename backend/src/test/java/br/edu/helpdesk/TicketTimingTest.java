package br.edu.helpdesk;

import br.edu.helpdesk.service.TicketTiming;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class TicketTimingTest {
 private static final Instant OPEN=Instant.parse("2026-10-16T12:00:00Z");
 private Map<String,Object> ticket(String status,int picked,int finished){
  var t=new HashMap<String,Object>();t.put("abertura",OPEN.toString());t.put("atendimento",picked<0?null:OPEN.plusSeconds(picked).toString());t.put("finalizacao",finished<0?null:OPEN.plusSeconds(finished).toString());t.put("status",status);t.put("sla_resposta",5);t.put("sla_resolucao",10);return t;
 }
 @Test void openTicketOnlyUsesResponseBudget(){var t=ticket("ABERTO",-1,-1);TicketTiming.decorate(t,OPEN.plusSeconds(120));assertEquals(180L,t.get("resposta_restante_segundos"));assertEquals(600L,t.get("resolucao_restante_segundos"));assertNull(t.get("prazo_finalizar"));assertEquals(false,t.get("fora_sla"));}
 @Test void resolutionStartsAtPickupInsteadOfOpening(){var t=ticket("EM_ATENDIMENTO",240,-1);TicketTiming.decorate(t,OPEN.plusSeconds(700));assertEquals(60L,t.get("resposta_restante_segundos"));assertEquals(140L,t.get("resolucao_restante_segundos"));assertEquals(false,t.get("fora_sla"));assertEquals(OPEN.plusSeconds(840).toString(),t.get("prazo_finalizar"));}
 @Test void deadlinesShowNegativeOverdueAndWaitingKeepsResolutionRunning(){var t=ticket("AGUARDANDO_USUARIO",120,-1);TicketTiming.decorate(t,OPEN.plusSeconds(800));assertEquals(-80L,t.get("resolucao_restante_segundos"));assertEquals(true,t.get("fora_sla"));var open=ticket("ABERTO",-1,-1);TicketTiming.decorate(open,OPEN.plusSeconds(320));assertEquals(-20L,open.get("resposta_restante_segundos"));assertEquals(true,open.get("fora_sla"));}
 @Test void terminalTimersFreezeAtClosingAndCancelledNeverCountsAsLate(){var t=ticket("FINALIZADO",200,500);TicketTiming.decorate(t,OPEN.plusSeconds(10000));assertEquals(300L,t.get("tempo_atendimento_segundos"));assertEquals(500L,t.get("tempo_total_segundos"));assertEquals(false,t.get("fora_sla"));var cancelled=ticket("CANCELADO",-1,1000);TicketTiming.decorate(cancelled,OPEN.plusSeconds(10000));assertEquals(1000L,cancelled.get("tempo_total_segundos"));assertEquals(false,cancelled.get("fora_sla"));}
}
