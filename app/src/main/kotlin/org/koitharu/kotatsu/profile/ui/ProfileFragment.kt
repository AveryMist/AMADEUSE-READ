package org.koitharu.kotatsu.profile.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import org.koitharu.kotatsu.core.nav.router
import org.koitharu.kotatsu.databinding.FragmentProfileBinding
import org.koitharu.kotatsu.settings.SettingsActivity

@AndroidEntryPoint
class ProfileFragment : Fragment() {

	private var _binding: FragmentProfileBinding? = null
	private val binding get() = _binding!!

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?
	): View {
		_binding = FragmentProfileBinding.inflate(inflater, container, false)
		return binding.root
	}

	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		setupButtons()
	}

	override fun onDestroyView() {
		super.onDestroyView()
		_binding = null
	}

	private fun setupButtons() {
		binding.buttonSettings.setOnClickListener {
			router.openSettings()
		}

		binding.buttonBackup.setOnClickListener {
			// Open backup settings through settings activity
			startActivity(Intent(requireContext(), SettingsActivity::class.java)
				.setAction("org.koitharu.kotatsu.action.BACKUP"))
		}

		binding.buttonDownloads.setOnClickListener {
			router.openDownloads()
		}

		binding.buttonBookmarks.setOnClickListener {
			router.openBookmarks()
		}

		binding.buttonAbout.setOnClickListener {
			// Open about settings through settings activity
			startActivity(Intent(requireContext(), SettingsActivity::class.java)
				.setData(Uri.parse("kotatsu://settings/about")))
		}

		binding.buttonStatistics.setOnClickListener {
			router.openStatistic()
		}
	}
}
