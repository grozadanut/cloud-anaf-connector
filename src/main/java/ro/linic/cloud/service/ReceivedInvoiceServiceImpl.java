package ro.linic.cloud.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.helger.ubl21.UBL21Marshaller;

import lombok.NonNull;
import lombok.extern.java.Log;
import oasis.names.specification.ubl.schema.xsd.creditnote_21.CreditNoteType;
import oasis.names.specification.ubl.schema.xsd.invoice_21.InvoiceType;
import ro.linic.cloud.entity.ReceivedCreditNote;
import ro.linic.cloud.entity.ReceivedInvoice;
import ro.linic.cloud.entity.ReceivedMessage;
import ro.linic.cloud.pojo.anaf.AnafReceivedMessage.AnafReceivedMessageType;
import ro.linic.cloud.repository.ReceivedCreditNoteRepository;
import ro.linic.cloud.repository.ReceivedInvoiceRepository;

@Service
@Log
public class ReceivedInvoiceServiceImpl implements ReceivedInvoiceService {
	@Autowired private ReceivedInvoiceRepository receivedInvoiceRepo;
	@Autowired private ReceivedCreditNoteRepository receivedCreditNoteRepo;
	@Autowired private AnafApi anafApi;
	
	@Override
	public void billReceived(@NonNull final OAuth2AccessToken anafAccessToken, @NonNull final ReceivedMessage message) {
		if (!message.getMessageType().equals(AnafReceivedMessageType.BILL_RECEIVED))
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		
		if (receivedInvoiceRepo.findById(message.getId()).isPresent())
			return;
		
		final String downloadId = message.getId().toString();
		final ResponseEntity<byte[]> downloadZipResponse = anafApi.downloadResponse(anafAccessToken, downloadId);
		
		if (!downloadZipResponse.getStatusCode().is2xxSuccessful())
			throw new ResponseStatusException(downloadZipResponse.getStatusCode());
		
		final byte[] zipFile = downloadZipResponse.getBody();
		final String rawXml = zipToXml(zipFile);
		
		try {
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document doc = db.parse(new ByteArrayInputStream(rawXml.getBytes("UTF-8")));
			final String docType = doc.getDocumentElement().getNodeName();
			
			if (StringUtils.equalsIgnoreCase(docType, "Invoice")) {
				final InvoiceType readInvoice = UBL21Marshaller.invoice().read(rawXml);
				final LocalDate issueDate = readInvoice.getIssueDateValueLocal();
				receivedInvoiceRepo.save(new ReceivedInvoice(message.getId(), message.getUploadIndex(), downloadId, rawXml, issueDate, null));
			} else if (StringUtils.equalsIgnoreCase(docType, "CreditNote")) {
				final CreditNoteType readCreditNote = UBL21Marshaller.creditNote().read(rawXml);
				final LocalDate issueDate = readCreditNote.getIssueDateValueLocal();
				receivedCreditNoteRepo.save(new ReceivedCreditNote(message.getId(), message.getUploadIndex(), downloadId, rawXml, issueDate));
			} else
				throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, docType + " document type not supported");
			
		} catch (final ParserConfigurationException | SAXException | IOException e) {
			log.log(Level.SEVERE, "Error parsing XML", e);
			log.log(Level.SEVERE, rawXml);
		}
	}
	
	private String zipToXml(final byte[] zipData) {
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipData))) {
			ZipEntry zipEntry;
			int read;
			final byte[] buffer = new byte[1024];
			final StringBuilder sb = new StringBuilder();
			
			while ((zipEntry = zis.getNextEntry()) != null) {
				if (!zipEntry.getName().toLowerCase().endsWith(".xml") ||
						zipEntry.getName().toLowerCase().startsWith("semnatura"))
					continue;
					
				while ((read = zis.read(buffer, 0, 1024)) >= 0)
					sb.append(new String(buffer, 0, read));
			}
			return sb.toString();
		} catch (final IOException e) {
			log.log(Level.SEVERE, "Extracting zip contents failed", e);
			return null;
		}
	}
}
