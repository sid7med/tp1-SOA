package mr.supnum.bib.impl;

import com.sun.xml.ws.developer.SchemaValidation;
import jakarta.jws.WebService;
import java.time.LocalDate;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import mr.supnum.bib.contrat.EmpruntPortType;
import mr.supnum.bib.contrat.EmpruntType;
import mr.supnum.bib.contrat.LivreIndisponible;
import mr.supnum.bib.contrat.LivreIndisponibleFault;

@SchemaValidation
@WebService(serviceName = "EmpruntService", portName = "EmpruntPort",
        targetNamespace = "http://supnum.mr/bib",
        endpointInterface = "mr.supnum.bib.contrat.EmpruntPortType",
        wsdlLocation = "wsdl/EmpruntService.wsdl")
public class EmpruntServiceImpl implements EmpruntPortType {
    @Override
    public EmpruntType emprunterLivre(String idAdherent, String isbn) throws LivreIndisponibleFault {
        if (isbn.equals("978-2-10-074444-4")) {
            LivreIndisponible info = new LivreIndisponible();
            info.setIsbn(isbn);
            info.setRetourPrevu(date(LocalDate.of(2026, 12, 2)));
            throw new LivreIndisponibleFault("Livre indisponible", info);
        }
        EmpruntType e = new EmpruntType();
        e.setNumero("E-2026-0812");
        e.setDateRetour(date(LocalDate.now().plusDays(14)));
        return e;
    }

    private static XMLGregorianCalendar date(LocalDate d) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(d.toString());
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }
}
