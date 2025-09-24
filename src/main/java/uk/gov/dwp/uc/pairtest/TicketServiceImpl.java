package uk.gov.dwp.uc.pairtest;

import uk.gov.dwp.uc.pairtest.constants.Constants;
import uk.gov.dwp.uc.pairtest.constants.ErrorMessage;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;

public class TicketServiceImpl implements TicketService {

	private static final Logger log = LoggerFactory.getLogger(TicketServiceImpl.class);

	private final TicketPaymentService paymentService;
	private final SeatReservationService reservationService;

	public TicketServiceImpl(TicketPaymentService paymentService, SeatReservationService reservationService) {
		this.paymentService = paymentService;
		this.reservationService = reservationService;
	}

	@Override
	public void purchaseTickets(Long accountId, TicketTypeRequest... ticketTypeRequests) {

		log.info("Purchase request received for accountId={} with {} ticket types",
				accountId, ticketTypeRequests != null ? ticketTypeRequests.length : 0);

		validateInput(accountId, ticketTypeRequests);

		int totalTicketsCount = 0;
		int totalAmount = 0;
		int seatsToReserve = 0;

		int adultTicketCount = 0;
		int childTicketCount = 0;
		int infantTicketCount = 0;


		for (TicketTypeRequest request : ticketTypeRequests) {
			if (request == null || request.getNoOfTickets() < 0) {
				throw new InvalidPurchaseException(ErrorMessage.NEGATIVE_TICKET.message());
			}
			int count = request.getNoOfTickets();
			totalTicketsCount += count;
			switch (request.getTicketType()) {
			case ADULT -> {
				totalAmount += count * Constants.ADULT_PRICE;
				seatsToReserve += count;
				adultTicketCount += request.getNoOfTickets();
			}
			case CHILD -> {
				totalAmount += count * Constants.CHILD_PRICE;
				seatsToReserve += count;
				childTicketCount += request.getNoOfTickets();
			}
			case INFANT -> {
				// Infants do not pay for a ticket and are not allocated a seat.
				// totalAmount += count * Constants.INFACT_PRICE;
				// seatsToReserve += count;
				infantTicketCount += request.getNoOfTickets();
			}
			}
		}

		log.info("Total Ticket Count is adult {} + child {} + infant {} = {}",adultTicketCount, childTicketCount, infantTicketCount, totalTicketsCount);

		// Child and Infant tickets cannot be purchased without purchasing an Adult ticket.
		if ((childTicketCount > 0 || infantTicketCount > 0) && adultTicketCount == 0) {
			log.error("Purchase failed: {}", ErrorMessage.CHILD_INFANT_WITHOUT_ADULT);
			throw new InvalidPurchaseException(ErrorMessage.CHILD_INFANT_WITHOUT_ADULT.message());
		}

		// Only a maximum of 25 tickets that can be purchased at a time.
		if (totalTicketsCount > Constants.MAX_TICKETS) {
			log.error("Purchase failed: {}", ErrorMessage.MAX_TICKETS_EXCEEDED);
			throw new InvalidPurchaseException(ErrorMessage.MAX_TICKETS_EXCEEDED.message());
		}

		log.info("Making payment of £{} for accountId={}", totalAmount, accountId);
		paymentService.makePayment(accountId, totalAmount);

		log.info("Reserving {} seat(s) for accountId={}", seatsToReserve, accountId);
		reservationService.reserveSeat(accountId, seatsToReserve);
		
		log.info("Purchase completed successfully for accountId={}", accountId);
	}

	private void validateInput(Long accountId, TicketTypeRequest[] requests) {
		if (accountId == null || accountId <= 0) {
			log.error("Invalid accountId: {}", accountId);
			throw new InvalidPurchaseException(ErrorMessage.INVALID_ACCOUNT.message());
		}

		if (requests == null || requests.length == 0) {
			log.error("Number of tickets is empty");
			throw new InvalidPurchaseException(ErrorMessage.EMPTY_REQUEST.message());
		}
	}
}
