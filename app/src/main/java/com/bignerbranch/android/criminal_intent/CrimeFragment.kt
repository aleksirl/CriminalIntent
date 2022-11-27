package com.bignerbranch.android.criminal_intent

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import java.util.*

private const val TAG = "CrimeFragment"
private const val ARG_CRIME_ID = "crime_id"
private const val DIALOG_DATE = "DialogDate"
private const val REQUEST_DATE = "0"
private const val DATE_FORMAT = "EEE, MMM, dd"
private const val REQUEST_CONTACT = "1"
class CrimeFragment: Fragment() {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var phoneButton: Button

    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProviders.of(this).get(CrimeDetailViewModel::class.java)
    }
    private val selectSuspect = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        uri?.let { pickContact(it) }
    }
    private val selectPhone = registerForActivityResult(
        ActivityResultContracts.PickContact()
    ){ uri: Uri? ->
        uri?.let { pickPhone(it) }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()
        val crimeId: UUID = arguments?.getSerializable(ARG_CRIME_ID) as UUID
        //Log.d(TAG,"args bundle crime ID: $crimeId")
        crimeDetailViewModel.loadCrime(crimeId)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime, container, false)
        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button
        phoneButton = view.findViewById(R.id.crime_call) as Button



        return view
    }
//Публикация новых данных
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
            }
            override fun onTextChanged(sequence: CharSequence?, start: Int, count: Int, after: Int) {
                crime.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?) {
            }
        }
        titleField.addTextChangedListener(titleWatcher)

        solvedCheckBox.apply {
            setOnCheckedChangeListener { _,isChecked -> crime.isSolved = isChecked }
        }
        //Нужно key к бандл ввывести в отдельные костанты
        dateButton.setOnClickListener {
            childFragmentManager.setFragmentResultListener("requestKey", viewLifecycleOwner){
                _,bundle ->
                val result = bundle.getSerializable("bundleKey") as Date
                crime.date = result
                updateUI()
            }
            //updateUI()
            DatePickerFragment.newInstance(crime.date).apply {
                show(this@CrimeFragment.childFragmentManager, DIALOG_DATE)
            }
        }
        reportButton.setOnClickListener{
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject))
            }.also { intent ->
                val chooserIntent =
                    Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent) }
        }
        suspectButton.apply {
            val pickContactIntent = Intent (Intent.ACTION_PICK, ContactsContract.AUTHORITY_URI)
            setOnClickListener{
                selectSuspect.launch(null)
            }
            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            //if (resolvedActivity == null) {
           //    isEnabled = false
            //}
        }
        phoneButton.setOnClickListener {

            if (crime.phone.isNotBlank()) {

                val call = Uri.parse("tel:${crime.phone}")
                val surf = Intent(Intent.ACTION_DIAL, call)
                startActivity(surf)
            } else {selectPhone.launch(null)}
          //  val callContactIntent =
           //     Intent(Intent.ACTION_DIAL).apply {

            //        val phone = crime.phone
                //    data = Uri.parse("tel:$phone")
                //    selectPhone.launch(null)

           //     }
                    //selectPhone.launch(null)
        }


    }


    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

    private fun updateUI(){
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
        suspectButton.text = crime.suspect.ifEmpty {
            getString(R.string.crime_suspect_text)
        }
    }
    private fun getCrimeReport(): String {
        val solvedString = if (crime.isSolved){
            getString(R.string.crime_report_solved)
        } else {getString(R.string.crime_report_unsolved)}
        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()
        var suspect = if (crime.suspect.isBlank()){
            getString(R.string.crime_report_no_suspect)
        } else {getString(R.string.crime_report_suspect, crime.suspect) }
        return getString(R.string.crime_report, crime.title, dateString, solvedString, suspect)

    }

    private fun pickContact (contactUri: Uri) {
        val queryField = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
        val cursor = contactUri.let {
            requireActivity().contentResolver.query(it, queryField, null, null,null)
        }
        cursor?.use {
            if (it.count > 0){
                it.moveToFirst()
                val suspect = it.getString(0)
                crime.suspect = suspect
                crimeDetailViewModel.saveCrime(crime)
                suspectButton.text = suspect
            }
        }
    }
    private fun pickPhone (contactUri: Uri){
        val queryField = arrayOf(ContactsContract.Contacts._ID)
        val cursor = contactUri.let {
            requireActivity().contentResolver.query(it,queryField, null, null, null)
        }
        cursor?.use {
            if (it.count > 0){
                it.moveToFirst()
                val phone = it.getString(0)
                val phoneURI = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
                val numberQueryPhone = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val phoneWhereClause = "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?"
                val phoneParams = arrayOf(phone)
                val phoneCursor = requireActivity().contentResolver.query(phoneURI,numberQueryPhone,phoneWhereClause,phoneParams,null)
                phoneCursor.use { cursorPhone ->
                    cursorPhone?.moveToFirst()
                    val phoneNumValue = cursorPhone?.getString(0)
                    if (phoneNumValue != null) {
                        crime.phone = phoneNumValue
                    }
                    phoneButton.text = phoneNumValue
                }


                crimeDetailViewModel.saveCrime(crime)
            }
        }




    }
    companion object {
        fun newInstance(crimeId: UUID): CrimeFragment {
            val args = Bundle().apply {
                putSerializable(ARG_CRIME_ID, crimeId)
            }
            return CrimeFragment().apply {
                arguments = args
            }
        }
    }
}



